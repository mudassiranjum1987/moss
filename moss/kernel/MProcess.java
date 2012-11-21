/*
 *	MProcess.java -- MOSS process description
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
import moss.ipc.*;
import moss.user.*;

/**
 * The MProcess class is effectively the process control block,
 * it extends Thread so it can exist on its own.
 */

public class MProcess extends Thread
{
	//{{{  variables
	/** next process on a wait queue */
	public MProcess q_next;

	/** prev_task and next_task are a linked list of all processes */
	public MProcess prev_task, next_task;

	/** state holds the process's state (STOPPED, RUNNING, etc.) */
	public int state;

	/** signal-handling states */
	public int sig_handling[];
	/** queue of pending signals */
	public MSignal pending_signals;
	/** true if the process has been signalled (allows drivers/etc. to return EINTR) */
	public boolean signalled;

	/** process id */
	public int pid;

	/** parent process id -- this is not used for active processes */
	public int ppid;

	/** link to parent process */
	public MProcess parent;

	/** if true, a kernel-only process */
	public boolean ktask;			/* true if a purely kernel task */

	/** array of open files (indexed by descriptor) */
	public MFile files[];			/* open files */

	/** handle on a user-process */
	public MUserProcess user_if;
	/** handle on a system-process */
	public MKernelProcess kernel_if;

	/** command-line arguments */
	public String cmdline[];

	/** a semaphore used to synchronize process startup */
	public Semaphore start_sem;

	/** string indicating what system-call the process is doing */
	public String syscall;

	/** the process's file creation mask */
	public int umask;

	/** process environment */
	public MEnv environ;

	/** link to the proc-filesystem info handler */
	public PFS_mprocess pfslink;


	//}}}
	//{{{  process state constants
	public static final int TASK_INVALID = 0;
	public static final int TASK_STOPPED = 1;
	public static final int TASK_RUNNABLE = 2;
	public static final int TASK_RUNNING = 3;
	public static final int TASK_SLEEPING = 4;
	public static final int TASK_FINISHED = 5;
	public static final int TASK_ZOMBIE = 6;
	//}}}
	//{{{  process creation flags
	public static final int INHERIT_OPEN_FILES = 0x0001;
	//}}}


	//{{{  private class PFS_mprocess implements MProcFSIf
	/**
	 * this class is used to report per-process statistics (in proc-fs)
	 */
	private class PFS_mprocess implements MProcFSIf
	{
		//{{{  private attributes
		/** associated process */
		private MProcess p;
		/** inode for the "cmdline" file */
		private MInode i_cmdline;
		/** inode for the "status" file */
		private MInode i_status;


		//}}}
		//{{{  public PFS_mprocess (MProcess p)
		/**
		 * constructs a new process information handler
		 *
		 * @param p process this is for
		 */
		public PFS_mprocess (MProcess p)
		{
			this.p = p;
			this.i_cmdline = null;
			this.i_status = null;
		}
		//}}}
		//{{{  public int register_entries ()
		/**
		 * does the various registrations with the process file-system
		 *
		 * @return 0 on success, otherwise &lt; 0 indicating error
		 */
		public int register_entries ()
		{
			String path = "" + p.pid;
			int r;

			r = MProcFS.register_procdir (path);
			if (r < 0) {
				return r;
			}

			i_cmdline = MProcFS.register_procimpl (path + "/cmdline", this);
			i_status = MProcFS.register_procimpl (path + "/status", this);

			return 0;
		}
		//}}}
		//{{{  public int unregister_entries ()
		/**
		 * unregisters the various files in the process file-system
		 *
		 * @return 0 on success, otherwise &lt; 0 indicating error
		 */
		public int unregister_entries ()
		{
			String path = "" + p.pid;
			int r;

			r = MProcFS.unregister_procimpl (path + "/cmdline");
			if (r < 0) {
				return r;
			}
			r = MProcFS.unregister_procimpl (path + "/status");
			if (r < 0) {
				return r;
			}

			r = MProcFS.unregister_procdir (path);
			
			return r;
		}
		//}}}
		//{{{  public String readproc (MInode inode, String name)
		/**
		 * called to read the contents of one of the proc-fs entries
		 *
		 * @param inode associated inode in the proc-fs
		 * @param name name of this entry
		 *
		 * @return a string containing the required information
		 */
		public String readproc (MInode inode, String name)
		{
			String r = "";

			if (inode == i_cmdline) {
				int i;

				for (i=0; i<p.cmdline.length; i++) {
					r = r + p.cmdline[i] + " ";
				}
				r = r + "\n";
			} else if (inode == i_status) {
				r = r + "name: " + p.getName() + "\n";
				r = r + "pid: " + p.pid + "\n";
				r = r + "ppid: " + p.ppid + "\n";
				r = r + "state: ";
				switch (p.state) {
				case MProcess.TASK_INVALID:
					r = r + "invalid\n";
					break;
				case MProcess.TASK_STOPPED:
					r = r + "stopped\n";
					break;
				case MProcess.TASK_RUNNABLE:
					r = r + "runnable\n";
					break;
				case MProcess.TASK_RUNNING:
					r = r + "running\n";
					break;
				case MProcess.TASK_SLEEPING:
					r = r + "sleeping\n";
					break;
				case MProcess.TASK_FINISHED:
					r = r + "finished\n";
					break;
				case MProcess.TASK_ZOMBIE:
					r = r + "zombie\n";
					break;
				}
				r = r + "syscall: " + ((p.syscall == null) ? "(none)" : p.syscall) + "\n";
			} else {
				MKernel.log_msg ("MProcess (PFS_mprocess): unhandled request to readproc(" + name + ")");
			}

			return r;
		}
		//}}}
	}
	//}}}
	//{{{  private class TermProcess extends Throwable
	/**
	 * this class is used to force a process to terminate, by raising this as an exception (throwable)
	 */
	private class TermProcess extends RuntimeException
	{
		public int exitcode;

		public TermProcess ()
		{
			super ("TERMINATED");
		}
	}
	//}}}

	
	//{{{  public MProcess ()
	/**
	 * MProcess constructor.  Should not be used for constructing
	 * things which will become processes;  use the other constructor for that.
	 */
	public MProcess ()
	{
		q_next = null;
		prev_task = null;
		next_task = null;
		state = TASK_INVALID;
		pid = 0;
		ktask = false;
		sig_handling = null;
		pending_signals = null;
		signalled = false;
		parent = null;
 		files = null;
		user_if = null;
		kernel_if = null;
		start_sem = null;
		cmdline = null;
		umask = 0002;
		environ = null;
		pfslink = null;
	}
	//}}}
	//{{{  public MProcess (MProcess parent)
	/**
	 * MProcess constructor
	 *
	 * @param parent parent process
	 */
	public MProcess (MProcess parent)
	{
		q_next = null;
		prev_task = null;
		next_task = null;
		state = TASK_INVALID;
		pid = 0;
		ktask = false;
		sig_handling = new int[MSignal.SIG_NSIGS];
		for (int i=0; i<MSignal.SIG_NSIGS; i++) {
			sig_handling[i] = MSignal.SIG_DFL;
		}
		pending_signals = null;
		signalled = false;
		this.parent = parent;
		user_if = null;
		kernel_if = null;
		start_sem = new Semaphore (0);
		syscall = null;

 		files = new MFile[MConfig.max_files_per_process];
		for (int i = 0; i < files.length; i++) {
			files[i] = null;
		}
		cmdline = new String[0];
		environ = new MEnv ();
		if ((parent != null) && (parent.environ != null) && (parent.environ.env != null)) {
			environ.env = (String[])parent.environ.env.clone();
		} else {
			environ.env = new String[0];
		}
		pfslink = null;
	}
	//}}}
	//{{{  public static int create_user_process (String name, MProcess parent, String args[], int flags)
	/**
	 * creates a new user process and adds it to the run-queue
	 *
	 * @param name name of the class that provides the process
	 * @param parent parent process
	 * @param args arguments to be passed to the new process
	 * @param flags process creation flags (INHERIT_...)
	 *
	 * @return pid of new process on success, -1 on failure
	 */
	public static int create_user_process (String name, MProcess parent, String args[], int flags)
	{
		Class uclass = null;
		MProcess mp;
		MUserProcess mup;

		if (name.charAt(0) == '/') {
			/* trying to run something from the file-system (need MExec's help) */
			MExec exec = null;
			int r;

			exec = MExec.load_active (name);
			if (exec == null) {
				/* doesn't already exist, create it */
				exec = new MExec ();
				r = exec.setpath (name);
				if (r < 0) {
					return r;
				}
			}
			if (exec.typeof() == MExec.EXEC_SCRIPT) {
				String strs[] = exec.shebangof ();

				if ((strs == null) || (strs.length < 2) || (strs[0] == null) || (strs[0].charAt(0) != '/')) {
					return -MSystem.ENOEXEC;
				}

				/* re-create the MExec object with updated info */
				name = strs[0];
				args = strs;
				exec = new MExec ();

				r = exec.setpath (name);
				if (r < 0) {
					return r;
				}
			}
			
			if (exec.typeof() == MExec.EXEC_CLASS) {
				ClassLoader cloader = exec.getclassloader ();
				String ncname = exec.classnameof ();

				if (cloader == null) {
					return -MSystem.ENOEXEC;
				}

				try {
					uclass = Class.forName (ncname, true, cloader);
				} catch (ClassNotFoundException e) {
					/* probably because ncname was junk, but now look in the class loader for a good name */
					ncname = exec.classnameof ();
					try {
						uclass = Class.forName (ncname, true, cloader);
					} catch (ClassNotFoundException f) {
						uclass = null;
						return -MSystem.ENOEXEC;
					}
				}
				
				if (uclass == null) {
					return exec.errorof ();
				}
			} else {
				return -MSystem.ENOEXEC;
			}

			/* FIXME: need to do this better.. (incl. trash_active() support) */
			MExec.store_active (exec, name);
		} else {
			try {
				uclass = Class.forName (name);
			} catch (ClassNotFoundException e) {
				uclass = null;
			}
		}
		if (uclass == null) {
			return -MSystem.ENOENT;
		}

		try {
			mup = (MUserProcess)(uclass.newInstance());
		} catch (Exception e) {
			return -MSystem.ENOEXEC;
		}

		/* create and initialise a new MProcess structure */
		mp = MKernel.NewProcess(parent);
		mp.user_if = mup;
		mp.pid = MKernel.get_free_pid ();
		mp.cmdline = args;

		/* inherit open files */
		if ((flags & INHERIT_OPEN_FILES) != 0) {
			for (int i=0; i<parent.files.length; i++) {
				if (parent.files[i] != null) {
					mp.files[i] = parent.files[i];
					mp.files[i].refcount++;
				} else {
					mp.files[i] = null;
				}
			}
		}

		mp.setName (name);
		MKernel.start_process (mp);
		/* by the time we get back here, it should be on the run-queue..! */
		return mp.pid;
	}
	//}}}
	//{{{  public static int create_kernel_process (String name, String args[])
	/**
	 * creates a new kernel process and adds it to the run-queue
	 *
	 * @param name name of the class that provides the process
	 * @param args arguments to be passed to the new process
	 *
	 * @return pid of new process on success, -1 on failure
	 */
	public static int create_kernel_process (String name, String args[])
	{
		Class kclass = null;
		MProcess mp;
		MKernelProcess mkp;
		String ncname = null;

		if (name.charAt(0) == '/') {
			/* trying to run something from the file-system (need MExec's help) */
			MExec exec = new MExec ();
			int r;

			r = exec.setpath (name);
			if (r < 0) {
				return r;
			}
			if (exec.typeof() == MExec.EXEC_CLASS) {
				ClassLoader cloader = exec.getclassloader ();
				ncname = exec.classnameof ();

				if (cloader == null) {
					return -MSystem.ENOEXEC;
				}

				try {
					kclass = Class.forName (ncname, true, cloader);
				} catch (ClassNotFoundException e) {
					/* probably because ncname was junk, but now look in the class loader for a good name */
					ncname = exec.classnameof ();
					try {
						kclass = Class.forName (ncname, true, cloader);
					} catch (ClassNotFoundException f) {
						kclass = null;
						return -MSystem.ENOEXEC;
					}
				}
				
				if (kclass == null) {
					return exec.errorof ();
				}
			} else {
				return -MSystem.ENOEXEC;
			}
		} else {
			try {
				kclass = Class.forName (name);
			} catch (ClassNotFoundException e) {
				kclass = null;
			}
		}
		if (kclass == null) {
			return -MSystem.ENOENT;
		}

		try {
			mkp = (MKernelProcess)(kclass.newInstance());
		} catch (Exception e) {
			return -MSystem.ENOEXEC;
		}

		/* create and initialise a new MProcess structure */
		mp = new MProcess ();
		mp.ktask = true;
		mp.kernel_if = mkp;
		mp.pid = MKernel.get_free_pid ();
		mp.cmdline = new String[args.length + 2];
		System.arraycopy (args, 0, mp.cmdline, 2, args.length);
		mp.cmdline[0] = ncname;
		mp.cmdline[1] = name;
		mp.start_sem = new Semaphore (0);
 		mp.files = new MFile[MConfig.max_files_per_process];
		for (int i = 0; i < mp.files.length; i++) {
			mp.files[i] = null;
		}

		mp.setName (name);
		MKernel.start_process (mp);

		/* by the time we get back here, it should be on the run-queue..! */
		return mp.pid;
	}
	//}}}
	//{{{  public static void shutdown_process (MProcess p)
	/**
	 * this is used to tidy-up a process that's shutting down.
	 *
	 * @param p process shutting down
	 */
	public static void shutdown_process (MProcess p)
	{
		/* trash any external stuff this process has */
		MMailBox.deadprocess (p.pid);

		/* close open files */
		for (int i = 0; i<p.files.length; i++) {
			if (p.files[i] != null) {
				MFile fh = p.files[i];

				p.files[i] = null;
				fh.refcount--;
				if (fh.refcount == 0) {
					int error;
					
					if (fh.fileif != null) {
						fh.fileif.close (fh);
					} else if (fh.dirif != null) {
						fh.dirif.close (fh);
					} else {
						MKernel.log_msg ("MProcess::shutdown_process(" + p.pid + "): bad file-handle.");
					}

					/* if we errored, tuff.. :( */
				}
			}
		}
	}
	//}}}
	//{{{  public void terminate_process (int exitcode)
	/**
	 * this is called by MPosixIf's exit() to force a process to terminate
	 *
	 * @param exitcode exit-code of the process
	 */
	public void terminate_process (int exitcode)
	{
		TermProcess tp = new TermProcess ();

		tp.exitcode = exitcode;
		throw tp;
	}
	//}}}
	//{{{  public void run ()
	/**
	 * thread "run" method for an MProcess
	 */
	public void run ()
	{
		int exitcode = -1;

		MKernel.add_to_task_list (this);
		if (!ktask) {

			/* regular user process */
			if (user_if == null) {
				MKernel.panic ("MProcess::run() no user_if!");
			} else if (parent == null) {
				/* this is the init task -- special case */
				pfslink = new PFS_mprocess (this);
				pfslink.register_entries ();

				exitcode = user_if.main (cmdline, environ);
				pfslink.unregister_entries ();
				pfslink = null;

				/* we should never get here, because it means the init task terminated... */
				MKernel.panic ("MProcess::run() init task returned!");
			} else {
				boolean errored = false;

				/* register with the process file-system */
				pfslink = new PFS_mprocess (this);
				pfslink.register_entries ();

				MKernel.starting_process (this);
				try {
					exitcode = user_if.main (cmdline, environ);
				} catch (TermProcess p) {
					exitcode = p.exitcode;
				} catch (LinkageError e) {
					RuntimeException ee = new RuntimeException (e.getMessage ());

					ee.setStackTrace (e.getStackTrace());
					MKernel.process_fault (this, ee);
				} catch (RuntimeException e) {
					MKernel.process_fault (this, e);
					errored = true;
				}
				shutdown_process (this);

				/* unregister with the process file-system */
				pfslink.unregister_entries ();
				pfslink = null;
				
				MKernel.ending_process (this, (errored ? -MSignal.SIGSEGV : exitcode));
				/* when we get back here, we're a mostly dead process; just the Java thread left, and still on the kenel task-list */
			}
		} else {
			/* kernel process */
			if (kernel_if == null) {
				MKernel.panic ("MProcess::run() no kernel_if!");
			} else {
				boolean errored = false;
				int r;

				MKernel.starting_process (this);
				r = MModules.register_module (cmdline[0]);
				if (r == 0) {
					try {
						exitcode = kernel_if.main (cmdline);
					} catch (TermProcess p) {
						exitcode = p.exitcode;
					} catch (NoClassDefFoundError e) {
						MKernel.module_fault (this, new RuntimeException (e.getMessage()));
						errored = true;
					} catch (RuntimeException e) {
						MKernel.module_fault (this, e);
						errored = true;
					}
					MModules.unregister_module (cmdline[0]);
				} else {
					MKernel.log_msg ("module [" + cmdline[0] + "] already loaded!");
				}
				// System.err.println ("MProcess::run(): module [" + cmdline[0] + "] terminated");

				shutdown_process (this);
				MKernel.ending_process (this, (errored ? -MSignal.SIGSEGV : exitcode));
				/* mostly dead when we get back here */
			}
		}
		MKernel.remove_from_task_list (this);
	}
	//}}}
	//{{{  public static boolean sync_process_signals (MProcess p)
	/**
	 * this method is used by MPosixIf to deliver signals to a process.
	 * The body is synchronized (on the process) to prevent races with
	 * other processes (on different virtual CPUs) delivering signals
	 *
	 * @param p process to handle signals for
	 *
	 * @return true if signals were processed, false otherwise
	 */
	public static boolean sync_process_signals (MProcess p)
	{
		// System.err.println ("sync_process_signals(" + ((p == null) ? "null" : p.getName()) + ") currentCPU is " + MProcessor.currentCPU ());
		synchronized (p) {
			if (p.signalled) {
				p.syscall = "[signal]";
				MKernel.deliver_process_signals (p);
				return true;
			}
		}
		return false;
	}
	//}}}
}


