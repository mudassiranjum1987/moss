/*
 *	MInitTask.java -- MOSS "init" process
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

package moss.kernel;

import moss.fs.*;
import moss.user.*;
import moss.drivers.*;

import java.io.*;


/**
 * this is the "init" process.  The rest of the system starts from here
 */

public class MInitTask implements MUserProcess
{
	//{{{  private attributes
	/** handle on self */
	private MProcess me;

	/** environment */
	private MEnv env;

	/** java console */
	private MJavaConsole console;


	//}}}

	//{{{  private int setuphostfile (String srcfile, String dstfile)
	/**
	 * copies a file from the host environment into the MOSS file-system (somewhere)
	 *
	 * @param srcfile source file-name in the host (JVM) file-system
	 * @param dstfile destination file-name in the MOSS file-system
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	private int setuphostfile (String srcfile, String dstfile)
	{
		FileInputStream fio;
		int fd;

		// MKernel.log_msg ("setuphostfile(): srcfile=[" + srcfile + "], dstfile = [" + dstfile + "]");

		try {
			fio = new FileInputStream (srcfile);
		} catch (FileNotFoundException e) {
			return -MSystem.ENOENT;
		} catch (SecurityException e) {
			return -MSystem.EACCESS;
		}

		fd = MPosixIf.open (dstfile, MFileOps.OPEN_WRITE | MFileOps.OPEN_CREAT, 0755);
		if (fd < 0) {
			try {
				fio.close ();
			} catch (IOException e) { /* skip */ }
			return fd;
		}
		
		/* copy contents */
		for (;;) {
			byte buf[] = new byte[8192];
			int r;

			try {
				r = fio.read (buf);
				if (r == -1) {
					r = 0;	/* end-of-file */
				}
			} catch (IOException e) {
				r = -1;
			}

			if (r < 0) {
				MPosixIf.close (fd);
				try {
					fio.close ();
				} catch (IOException e) { /* skip */ }
				return -MSystem.EIO;
			} else if (r > 0) {
				/* write these bytes */
				int x;

				x = MPosixIf.write (fd, buf, r);
				if (x != r) {
					MPosixIf.close (fd);
					try {
						fio.close ();
					} catch (IOException e) { /* skip */ }
					return -MSystem.EIO;
				}
			} else {
				break;		/* end-of-file */
			}
		}

		MPosixIf.close (fd);
		try {
			fio.close ();
		} catch (IOException e) { /* skip */ }

		return 0;
	}
	//}}}
	//{{{  private int setuphostinv (String srcfile)
	/**
	 * reads the module inventory and calls setuphostfile() appropriately
	 *
	 * @param srcfile path to the inventory
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	private int setuphostinv (String srcfile)
	{
		FileInputStream fio;
		byte inv[] = null;
		int i;

		try {
			fio = new FileInputStream (srcfile);
		} catch (FileNotFoundException e) {
			return -MSystem.ENOENT;
		} catch (SecurityException e) {
			return -MSystem.EACCESS;
		}

		/* read inventory */
		for (;;) {
			byte buf[] = new byte[512];
			int r;

			try {
				r = fio.read (buf);
				if (r == -1) {
					r = 0;	/* end-of-file */
				}
			} catch (IOException e) {
				r = -1;
			}

			if (r < 0) {
				try {
					fio.close ();
				} catch (IOException e) { /* skip */ }
				return -MSystem.EIO;
			} else if (r > 0) {
				/* grab these bytes */
				if (inv == null) {
					inv = new byte[r];
					System.arraycopy (buf, 0, inv, 0, r);
				} else {
					byte newbuf[] = new byte[inv.length + r];

					System.arraycopy (inv, 0, newbuf, 0, inv.length);
					System.arraycopy (buf, 0, newbuf, inv.length, r);
					inv = newbuf;
				}
			} else {
				break;			/* end-of-file */
			}
		}

		try {
			fio.close ();
		} catch (IOException e) { /* skip */ }

		/* process inventory */
		for (i=0; i<inv.length;) {
			int j;
			byte tmpbuf[];
			String spath;
			String dpath;

			for (j=i; (j<inv.length) && (inv[j] != (byte)' '); j++);
			if ((j-i) == 0) {
				MPosixIf.writestring (MPosixIf.STDERR, "bad inventory!\n");
				return -MSystem.EINVAL;
			}
			tmpbuf = new byte[j-i];
			System.arraycopy (inv, i, tmpbuf, 0, j-i);
			spath = new String (tmpbuf);
			j++;
			for (i=j; (j<inv.length) && (inv[j] != (byte)'\n'); j++);
			if ((j-i) == 0) {
				MPosixIf.writestring (MPosixIf.STDERR, "bad inventory!\n");
				return -MSystem.EINVAL;
			}
			tmpbuf = new byte[j-i];
			System.arraycopy (inv, i, tmpbuf, 0, j-i);
			dpath = new String (tmpbuf);
			j++;
			i=j;

			dpath = dpath.replaceAll("[\\r\\n]", "");	//Remove carriage return that has been added by <manj>
			j = setuphostfile (spath, dpath);
			if (j < 0) {
				return j;
			}
		}

		return 0;
	}
	//}}}

	//{{{  public MInitTask (String bootargs[])
	/**
	 * constructor for the init-task
	 *
	 * @param bootargs boot arguments (MOSS command-line)
	 */
	public MInitTask (String bootargs[])
	{
		/* this is fairly specific to the init-task; other processes should not have
		 * access to their MProcess (although it's obviously gettable-at, but heyho..).
		 * Other (regular) processes should not start themselves either.. */
		console = null;
		env = new MEnv ();
		me = MKernel.NewProcess(null);

		me.pid = 1;
		me.state = MProcess.TASK_INVALID;
		me.user_if = this;
		me.cmdline = bootargs;
		me.setName ("MInitTask");

		MKernel.first_process (me);
	}
	//}}}
	//{{{  public void signal (int signo, Object sigdata)
	/**
	 * signal handler for the init-task
	 *
	 * @param signo signal number
	 * @param sigdata associated signal data (if any)
	 */
	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDOUT, "MInitTask::signal(" + signo + ") ");
		if (signo == MSignal.SIGCHLD) {
			int data[] = (int[])sigdata;

			MPosixIf.writestring (MPosixIf.STDOUT, "SIGCHLD (pid=" + data[0] + ", status=" + data[1] + ")\n");
		} else {
			MPosixIf.writestring (MPosixIf.STDOUT, "\n");
		}
		return;
	}
	//}}}
	//{{{  public int main (String args[], String env[])
	/**
	 * init-task process entry-point
	 *
	 * @param args command-line arguments
	 * @param env initial environment
	 *
	 * @return exit-code
	 */
	public int main (String args[], MEnv env)
	{
		MFile fh;
		int pid, x;
		String rootfstype = null;

		/* finalise kernel initialisation (in the context of the init-task) */
		MKernel.finalinit ();

		/* say hello */
		System.out.println ("hello world!  (from init) " + args.length + " args:");
		for (int i = 0; i<args.length; i++) {
			System.out.println ("    [" + args[i] + "]");
		}

		/* set default handling for all signals (ignore) */
		for (int i=0; i<MSignal.SIG_NSIGS; i++) {
			MPosixIf.signal (i, MSignal.SIG_IGN);
		}
		/* say we want to handle SIGCHLD */
		MPosixIf.signal (MSignal.SIGCHLD, MSignal.SIG_CATCH);

		/* start up the MJavaConsole which will provide basic I/O (to the tty we're sat on, if any..) */
		System.out.println ("creating MJavaConsole process..");
		console = new MJavaConsole ();

		/* create input, output and error descriptors (both output and error go to the same output) */
		fh = new MFile ();
		fh.refcount = 3;
		fh.fileif = console;
		console.open (fh, 0);			/* hackish open, can't use regular file open yet.. */
		me.files[MPosixIf.STDIN] = fh;
		me.files[MPosixIf.STDOUT] = fh;
		me.files[MPosixIf.STDERR] = fh;

		MPosixIf.reschedule ();

		/* load a device-driver */
		MPosixIf.writestring (MPosixIf.STDOUT, "loading ramdisk driver...\n");
		MDevices.load_driver ("MRamdisk");

		/* look for root=... command-line option */
		for (int i=0; i<me.cmdline.length; i++) {
			String arg = me.cmdline[i];

			if ((arg.length() >= 6) && (arg.substring (0, 5)).equals ("root=")) {
				rootfstype = arg.substring (5, arg.length());
				break;		/* for() */
			}
		}
		if (rootfstype == null) {
			/* default file-system type */
			rootfstype = "MObjFS";
		}

		MPosixIf.writestring (MPosixIf.STDOUT, "mounting root file-system... ");
		MPosixIf.reschedule ();
		x = MPosixIf.mount ("/", rootfstype, new String[] {});
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, "failed: " + MStdLib.strerror (x) + "\n");
		} else {
			MPosixIf.writestring (MPosixIf.STDOUT, "mounted type " + rootfstype + "\n");
		}

		MPosixIf.writestring (MPosixIf.STDOUT, "initialising root file-system... ");
		/* create a bare file-system */
		MPosixIf.mkdir ("/etc", 0755);
		MPosixIf.mkdir ("/dev", 0755);
		MPosixIf.mkdir ("/bin", 0755);
		MPosixIf.mkdir ("/lib", 0755);
		MPosixIf.mkdir ("/proc", 0755);
		MPosixIf.mkdir ("/host", 0755);
		MPosixIf.mkdir ("/modules", 0755);
		MPosixIf.writestring (MPosixIf.STDOUT, "done\n");

		MPosixIf.writestring (MPosixIf.STDOUT, "processing inventory for /bin, /modules and /lib... ");
		x = setuphostinv ("INVENTORY");
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, "failed.\n");
		} else {
			MPosixIf.writestring (MPosixIf.STDOUT, "done\n");
		}


		MPosixIf.writestring (MPosixIf.STDOUT, "mounting MProcFS on /proc... ");
		x = MPosixIf.mount ("/proc", "MProcFS", new String[] {});
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, "failed: " + MStdLib.strerror (x) + "\n");
		} else {
			MPosixIf.writestring (MPosixIf.STDOUT, "done\n");
		}


		MPosixIf.writestring (MPosixIf.STDOUT, "mounting MDevFS on /dev... ");
		x = MPosixIf.mount ("/dev", "MDevFS", new String[] {});
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, "failed: " + MStdLib.strerror (x) + "\n");
		} else {
			MPosixIf.writestring (MPosixIf.STDOUT, "done\n");
		}


		MPosixIf.writestring (MPosixIf.STDOUT, "mounting MHostFS on /host... ");
		x = MPosixIf.mount ("/host", "MHostFS", new String[] {});
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, "failed: " + MStdLib.strerror (x) + "\n");
		} else {
			MPosixIf.writestring (MPosixIf.STDOUT, "done\n");
		}


		MPosixIf.writestring (MPosixIf.STDOUT, "starting /bin/console... ");
		pid = MPosixIf.forkexec ("/bin/console", new String[] {"/bin/console"});
		if (pid < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, "failed: " + MStdLib.strerror (pid) + "\n");
			MPosixIf.writestring (MPosixIf.STDOUT, "note: system may be unusable..\n");
		} else {
			MPosixIf.writestring (MPosixIf.STDOUT, "done (pid " + pid + ")\n");
		}

		while (true) {
			/* sits in here forever, will transparently catch death of assorted child processes */
			MPosixIf.pause ();
		}
		// return 0;
	}
	//}}}
	
}


