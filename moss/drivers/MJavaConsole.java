/*
 *	MJavaConsole.java -- for interacting with the host terminal (System.{in,out})
 *	Copyright (C) 2004 Fred Barnes <frmb@kent.ac.uk>
 *
 *	This program is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 2 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this program; if not, write to the Free Software
 *	Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package moss.drivers;

import moss.fs.*;
import moss.kernel.*;
import moss.user.*;

import java.lang.*;
import java.io.*;

/**
 * this class provides a MFileOps interface to the Java in/out streams.
 * It is opened once by the init task when the system starts.
 */

public class MJavaConsole implements MFileOps
{
	/** local keyboard buffer */
	private byte read_buffer[];
	/** buffer state */
	private int bufsize, bufleft, bufin, bufhead, buftail;
	/** any waiting reader(s) */
	private MWaitQueue blocked_reader;
	/** suspended keyboard process */
	private InputThread ihook;
	/** indicator of blocking/non-blocking operation (as far as it works) */
	private boolean blocking;

	//{{{  private class InputThread extends Thread
	/**
	 * this class is used to read from the keyboard.  It doesn't run as a MOSS process,
	 * just on its own, happily reading from the keyboard and leaving the data in MJavaConsole
	 */
	private class InputThread extends Thread
	{
		private MJavaConsole console;
		private boolean eof;
		private boolean sleeping;

		//{{{  public InputThread (MJavaConsole console)
		/**
		 * constructor for the keyboard-process
		 *
		 * @param console console that provides the buffer
		 */
		public InputThread (MJavaConsole console)
		{
			this.console = console;
			this.eof = false;
			this.sleeping = false;
		}
		//}}}
		//{{{  public void run ()
		/**
		 * main thread of control for the "keyboard process"
		 */
		public void run ()
		{
			int v;
			byte lbuf[];

			lbuf = new byte[128];
			while (!eof) {
				//{{{  scoop up some input
				try {
					if (console.blocking) {
						v = System.in.read (lbuf);
					} else {
						v = System.in.read ();
						if (v >= 0) {
							lbuf[0] = (byte)v;
							v = 1;
						}
					}
					if (v == -1) {
						eof = true;
					}
				} catch (IOException e) {
					eof = true;
					v = -1;
				}
				if (v == 0) {
					/* this doesn't look friendly */
					eof = true;
				}
				//}}}
				while (!eof) {
					boolean do_sleep = false;

					synchronized (console) {
						//{{{  put in the keyboard process's buffer
						if (v > console.bufleft) {
							/* no room, sleep.. */
							console.ihook = this;
							do_sleep = true;
							sleeping = false;		/* to make sure */
						} else if ((console.bufhead + v) > console.bufsize) {
							/* two part copy */
							int chunk = (console.bufsize - console.bufhead);
							
							System.arraycopy (lbuf, 0, console.read_buffer, console.bufhead, chunk);
							System.arraycopy (lbuf, chunk, console.read_buffer, 0, v - chunk);
							console.bufhead += v;
							console.bufhead &= (console.bufsize - 1);
							console.bufin += v;
							console.bufleft -= v;
						} else {
							/* one part copy */
							System.arraycopy (lbuf, 0, console.read_buffer, console.bufhead, v);
							console.bufhead += v;
							console.bufhead &= (console.bufsize - 1);
							console.bufin += v;
							console.bufleft -= v;
						}
						//}}}
						//{{{  if the keyboard process has a waiting reader, wake it up
						if (!console.blocked_reader.is_empty() && !do_sleep) {
							/* wake up a blocked reader */
							MProcess p = console.blocked_reader.get_from_queue ();

							MKernel.add_to_run_queue (p);
						}
						//}}}
					}
					//{{{  either stored the data or ourself, sleep if needed or break while()
					if (do_sleep) {
						synchronized (this) {
							if (!sleeping) {
								sleeping = true;
								try {
									this.wait ();
								} catch (InterruptedException e) {
									/* do nothing */
								}
							}
						}
					} else {
						/* got something, break out of while() loop */
						break;
					}
					//}}}
				}
				/* and go round again for another go (!eof) */
			}
		}
		//}}}
	}
	//}}}

	//{{{  public MJavaConsole ()
	/**
	 * constructor; initialises buffer, etc.
	 */
	public MJavaConsole ()
	{
		InputThread kyb_p;

		bufsize = MConfig.pipe_buffer_size;
		read_buffer = new byte[bufsize];
		bufleft = bufsize;
		bufin = 0;
		bufhead = 0;
		buftail = 0;
		blocked_reader = new MWaitQueue ();
		ihook = null;
		blocking = true;

		/* start the keyboard process! */
		kyb_p = new InputThread (this);
		kyb_p.start ();

		/* register */
		MDevices.register_driver ("console", MConfig.DEV_MAJOR_CONSOLE, this);

		/* register with device file-system */
		MDevFS.register_device ("console", MConfig.DEV_MAJOR_CONSOLE, 0, MInode.S_IFCHR | 0666);
	}
	//}}}

	//{{{  public int open (MFile handle, int flags)
	/**
	 * called when initialising the console;  stores reference to self in the file-handle
	 *
	 * @param handle file-handle
	 * @param flags open flags (ignored)
	 *
	 * @return 0 on success or &lt; 0 on error
	 */
	public int open (MFile handle, int flags)
	{
		handle.pdata = (Object)this;
		return 0;
	}
	//}}}
	//{{{  public int close (MFile handle)
	/**
	 * called when closing the console -- should never be called..!
	 *
	 * @param handle file-handle
	 *
	 * @return 0 on success or &lt; 0 on error
	 */
	public int close (MFile handle)
	{
		MKernel.panic ("MJavaConsole::close() closing the console!");
		return 0;
	}
	//}}}
	//{{{  public int lseek (MFile handle, int offset, int whence)
	/**
	 * not supported by the console
	 */
	public int lseek (MFile handle, int offset, int whence)
	{
		return -MSystem.ESPIPE;
	}
	//}}}
	//{{{  public int read (MFile handle, byte buffer[], int count)
	/**
	 * called to read data from the keyboard
	 *
	 * @param handle file-handle
	 * @param buffer buffer where data will be stored
	 * @param count maximum number of bytes to read
	 *
	 * @return number of bytes read, 0 on end-of-file, or &lt; 0 on error
	 */
	public int read (MFile handle, byte buffer[], int count)
	{
		if (count > buffer.length) {
			return -MSystem.EFAULT;
		}
		while (true) {
			boolean do_sleep = false;
			MProcess current = MKernel.current[MProcessor.currentCPU()];

			synchronized (this) {
				if (bufin == 0) {
					//{{{  empty buffer, put reader to sleep
					synchronized (current) {
						blocked_reader.add_to_queue (current);
						current.state = MProcess.TASK_SLEEPING;
						do_sleep = true;
					}
					//}}}
				} else if (count >= bufin) {
					//{{{   can get the whole lot in one go
					/* can get the whole lot in one go */
//MKernel.log_msg ("MJavaConsole::read(): (whole lot) bufin = " + bufin + ", bufhead = " + bufhead + ", buftail = " + buftail);
					if (bufhead <= buftail) {
						/* two-part copy */
						
						System.arraycopy (this.read_buffer, buftail, buffer, 0, bufsize - buftail);
						System.arraycopy (this.read_buffer, 0, buffer, bufsize - buftail, bufhead);
					} else {
						/* one-part copy */
						System.arraycopy (this.read_buffer, buftail, buffer, 0, bufin);
					}
					count = bufin;
					bufhead = 0;
					buftail = 0;
					bufin = 0;
					bufleft = bufsize;
					//}}}
				} else {
					//{{{  can't get the whole lot, just get a bit
//MKernel.log_msg ("MJavaConsole::read(): (partial) bufin = " + bufin + ", bufhead = " + bufhead + ", buftail = " + buftail);
					if ((buftail + count) > bufsize) {
						/* two-part copy */
						System.arraycopy (this.read_buffer, buftail, buffer, 0, bufsize - buftail);
						System.arraycopy (this.read_buffer, 0, buffer, bufsize - buftail, count - (bufsize - buftail));
					} else {
						/* one-part copy */
						System.arraycopy (this.read_buffer, buftail, buffer, 0, count);
					}
					buftail += count;
					buftail &= MConfig.pipe_buffer_mask;
					bufin -= count;
					bufleft += count;
					//}}}
				}
				//{{{  if the keyboard process is waiting, resume it
				if (!do_sleep && (ihook != null)) {
					synchronized (ihook) {
						if (ihook.sleeping) {
							/* it really is sleeping */
							ihook.notify ();
						} else {
							/* it isn't yet, so don't let it */
							ihook.sleeping = true;
						}
						ihook = null;
					}
				}
				//}}}
			}
			//{{{  sleep if needed
			if (do_sleep) {
				boolean xsleep;

				synchronized (current) {
					xsleep = ((current.state == MProcess.TASK_SLEEPING) && !current.signalled);
				}
				if (xsleep) {
					/* reschedule */
					MKernel.schedule ();
				}
				if (current.signalled) {
					/* better get off the blocked reader queue */
					synchronized (this) {
						blocked_reader.del_from_queue (current);
					}
					return -MSystem.EINTR;
				}
			} else {
				return count;
			}
			//}}}
		}
	}
	//}}}
	//{{{  public int write (MFile handle, byte buffer[], int count)
	/**
	 * called to write data to the screen
	 *
	 * @param handle file handle
	 * @param buffer data to write
	 * @param count number of bytes to write
	 *
	 * @return number of bytes written or &lt; 0 on error
	 */
	public int write (MFile handle, byte buffer[], int count)
	{
		String str;
		
		if (count > buffer.length) {
			return -MSystem.EINVAL;
		}
		str = new String (buffer, 0, count);
		System.out.print (str);
		return count;
	}
	//}}}
	//{{{  public int fcntl (MFile handle, int op, int arg)
	/**
	 * file-handle control.  The only operation the console supports is to
	 * adjust the blocking/non-blocking setting.
	 *
	 * @param handle file-handle
	 * @param op operation (only F_SETFL supported here)
	 * @param arg operation-specific argument (new flags)
	 */
	public int fcntl (MFile handle, int op, int arg)
	{
		switch (op) {
		case MFileOps.F_SETFL:
			/* only interested in the O_NONBLOCK flag */
			if ((arg & MFileOps.O_NONBLOCK) != 0) {
				blocking = false;
			} else {
				blocking = true;
			}
			/* update descriptor flags */
			handle.flags = ((handle.flags & ~MFileOps.O_NONBLOCK) | (arg & MFileOps.O_NONBLOCK));
			return 0;
		}
		return -MSystem.EINVAL;
	}
	//}}}
}



