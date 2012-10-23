/*
 *	MPipe.java -- pipe-style communication primitive
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

package moss.ipc;

import moss.kernel.*;
import moss.fs.*;
import moss.user.*;

/**
 * This implements the "pipe" IPC mechanism
 */
public class MPipe implements MFileOps
{
	//{{{  private vars
	/** pipe buffer */
	private byte buffer[];

	/** buffer state */
	private int bufsize, bufleft, bufin, bufhead, buftail;

	/** waiting readers queue */
	private MWaitQueue blocked_reader;
	/** waiting writers queue */
	private MWaitQueue blocked_writer;

	/** reader(s) MFile handle */
	private MFile reader_handle;
	/** writer(s) MFile handle */
	private MFile writer_handle;

	//}}}

	//{{{  public MPipe ()
	/**
	 * initialises the pipe to its blank state
	 */
	public MPipe ()
	{
		bufsize = MConfig.pipe_buffer_size;
		buffer = new byte[bufsize];
		bufleft = bufsize;
		bufin = 0;
		bufhead = 0;
		buftail = 0;
		blocked_reader = new MWaitQueue ();
		blocked_writer = new MWaitQueue ();
		reader_handle = null;
		writer_handle = null;
	}
	//}}}
	//{{{  public int open (MFile handle, int flags)
	/**
	 * called when creating a pipe;  stores reference to self in the file-handle.
	 * to determine whether it is the reading or writer, `flags' is either OPEN_READ or OPEN_WRITE.
	 *
	 * @param handle file-handle
	 * @param flags open flags
	 *
	 * @return 0 on success or &lt; 0 on error
	 */
	public int open (MFile handle, int flags)
	{
		handle.pdata = (Object)this;
		if ((flags & MFileOps.OPEN_READ) != 0) {
			this.reader_handle = handle;
		} else if ((flags & MFileOps.OPEN_WRITE) != 0) {
			this.writer_handle = handle;
		} else {
			MKernel.log_msg ("MPipe::open(): bad usage.");
			return -MSystem.EIO;
		}
		return 0;
	}
	//}}}
	//{{{  public int close (MFile handle)
	/**
	 * called when closing (destroying) a pipe
	 *
	 * @param handle file-handle
	 *
	 * @return 0 on success or &lt; 0 on error
	 */
	public int close (MFile handle)
	{
		handle.pdata = null;
		synchronized (this) {
			if (handle == reader_handle) {
				reader_handle = null;
				/* wake up any blocked writers */
				while (!blocked_writer.is_empty ()) {
					MProcess p = blocked_writer.get_from_queue ();

					MKernel.add_to_run_queue (p);
				}
			} else if (handle == writer_handle) {
				writer_handle = null;
				/* wake up any blocked readers */
				while (!blocked_reader.is_empty ()) {
					MProcess p = blocked_reader.get_from_queue ();

					MKernel.add_to_run_queue (p);
				}
			} else {
				MKernel.log_msg ("MPipe::close(): handle not reader/writer.");
				return -MSystem.EIO;
			}
		}
		return 0;
	}
	//}}}
	//{{{  public int lseek (MFile handle, int offset, int whence)
	/**
	 * not supported by pipes
	 */
	public int lseek (MFile handle, int offset, int whence)
	{
		return -MSystem.ESPIPE;
	}
	//}}}
	//{{{  public int read (MFile handle, byte buffer[], int count)
	/**
	 * called to read data from a pipe
	 *
	 * @param handle file-handle
	 * @param buffer buffer where data will be stored
	 * @param count maximum number of bytes to read
	 *
	 * @return number of bytes read, 0 on end-of-file, or &lt; 0 on error
	 */
	public int read (MFile handle, byte buffer[], int count)
	{
		synchronized (this) {
			if (count > buffer.length) {
				return -MSystem.EFAULT;
			}
			if (handle != reader_handle) {
				return -MSystem.EBADF;
			}
		}
		while (true) {
			boolean do_sleep = false;
			MProcess current = MKernel.current[MProcessor.currentCPU()];

			synchronized (this) {
				if (bufin == 0) {
					/* check for end-of-file (writer closed pipe) */
					if (writer_handle == null) {
						count = 0;		/* end-of-file */
					} else {
						/* empty buffer, better sleep */
						synchronized (current) {
							blocked_reader.add_to_queue (current);
							current.state = MProcess.TASK_SLEEPING;
							do_sleep = true;
						}
					}
				} else if (count >= bufin) {
					/* can get the whole lot in one go */
					if (bufhead <= buftail) {
						/* two-part copy */
						System.arraycopy (this.buffer, buftail, buffer, 0, bufsize - buftail);
						System.arraycopy (this.buffer, 0, buffer, bufsize - buftail, bufhead);
					} else {
						/* one-part copy */
						System.arraycopy (this.buffer, buftail, buffer, 0, bufin);
					}
					count = bufin;
					bufhead = 0;
					buftail = 0;
					bufin = 0;
					bufleft = bufsize;
				} else {
					/* can't get the whole lot, just get a bit */
					if ((buftail + count) > bufsize) {
						/* two-part copy */
						System.arraycopy (this.buffer, buftail, buffer, 0, bufsize - buftail);
						System.arraycopy (this.buffer, 0, buffer, bufsize - buftail, count - (bufsize - buftail));
					} else {
						/* one-part copy */
						System.arraycopy (this.buffer, buftail, buffer, 0, count);
					}
					buftail += count;
					buftail &= MConfig.pipe_buffer_mask;
					bufin -= count;
					bufleft += count;
				}
				if (!blocked_writer.is_empty() && !do_sleep) {
					/* wake up a blocked writer */
					MProcess p = blocked_writer.get_from_queue ();

					MKernel.add_to_run_queue (p);
				}
			}
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
					/* remove from wait queue */
					synchronized (this) {
						blocked_reader.del_from_queue (current);
					}
					return -MSystem.EINTR;
				}
			} else {
				return count;
			}
		}
	}
	//}}}
	//{{{  public int write (MFile handle, byte buffer[], int count)
	/**
	 * called to write data into the pipe
	 *
	 * @param handle file handle
	 * @param buffer data to write
	 * @param count number of bytes to write
	 *
	 * @return number of bytes written or &lt; 0 on error
	 */
	public int write (MFile handle, byte buffer[], int count)
	{
		synchronized (this) {
			if (count > buffer.length) {
				return -MSystem.EFAULT;
			}
			if (count > bufsize) {
				count = bufsize;		/* only allow this much in onw go.. */
			}
			if (handle != writer_handle) {
				return -MSystem.EBADF;
			}
		}
		while (true) {
			MProcess current = MKernel.current[MProcessor.currentCPU()];
			boolean do_sleep = false;

			synchronized (this) {
				/* if the reader has closed the pipe, generate SIGPIPE and return EPIPE */
				if (reader_handle == null) {
					MKernel.queue_signal (current, new MSignal (MSignal.SIGPIPE, null));
					return -MSystem.EPIPE;
				}
				if (bufleft < count) {
					/* can't get it all in the buffer, sleep */
					synchronized (current) {
						blocked_writer.add_to_queue (current);
						current.state = MProcess.TASK_SLEEPING;
						do_sleep = true;
					}
				} else {
					/* can get it all in the buffer :) */
					if ((bufhead + count) > bufsize) {
						/* two-part copy */
						System.arraycopy (buffer, 0, this.buffer, bufhead, (bufsize - bufhead));
						System.arraycopy (buffer, (bufsize - bufhead), this.buffer, 0, count - (bufsize - bufhead));
					} else {
						/* one-part copy */
						System.arraycopy (buffer, 0, this.buffer, bufhead, count);
					}
					bufhead += count;
					bufhead &= MConfig.pipe_buffer_mask;
					bufin += count;
					bufleft -= count;
				}
				if (!blocked_reader.is_empty() && !do_sleep) {
					/* wake up a blocked reader */
					MProcess p = blocked_reader.get_from_queue ();

					MKernel.add_to_run_queue (p);
				}
			}
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
					/* remove from wait queue */
					synchronized (this) {
						blocked_writer.del_from_queue (current);
					}
					return -MSystem.EINTR;
				}
			} else {
				return count;
			}
		}
	}
	//}}}
	//{{{  public int fcntl (MFile handle, int op, int arg)
	/**
	 * not supported by pipes
	 */
	public int fcntl (MFile handle, int op, int arg)
	{
		return -MSystem.EIO;
	}
	//}}}
}


