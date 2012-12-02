/*
 *	MKernel.java -- MOSS kernel / start

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
import moss.drivers.*;
import moss.ipc.*;
import moss.kernel.Scheduler.IPriorityProcess;
import moss.kernel.Scheduler.IPriorityScheduler;
import moss.kernel.Scheduler.MPrioritizedLotteryProcess;
import moss.kernel.Scheduler.MPrioritizedProcess;
import moss.kernel.Scheduler.PrioritizedLotteryScheduler;
import moss.kernel.Scheduler.ProcessPriorityEnum;
import moss.kernel.Scheduler.RoundRobinPriorityScheduler;
import moss.user.*;

import java.lang.*;
import java.io.*;
import java.util.*;

public class MKernel
{
	//{{{  private stuff -- code must hold "lock" (below) before accessing these
	public static CREWLock lock;
	private static int nextpid;
	//}}}
	//{{{  public static variables that we allow other parts of the system to read

	/** array of active processes by CPU.  */
	public static MProcess current[];

	/** the special "init task" */
	public static MProcess init_task;

	/** task list */
	public static MProcess task_list;

	/** virtual processor objects */
	public static MProcessor processors[];
	
	/** Scheduler object */
	public static IScheduler m_schedular; 
	
	private static SchedulerType m_schedularType;
	
	//}}}
	
	public static IScheduler getScheduler()
	{
		return m_schedular;
	}
	

	//{{{  private static class PFS_mkernel implements MProcFSIf
	/**
	 * this class provides some basic information via the process file-system
	 */
	private static class PFS_mkernel implements MProcFSIf
	{
		//{{{  private attributes
		/** "version" file inode */
		private MInode i_version;
		/** "host" file inode */
		private MInode i_host;


		//}}}
		//{{{  public PFS_mkernel ()
		/**
		 * constructor
		 */
		public PFS_mkernel ()
		{
			i_version = MProcFS.register_procimpl ("version", this);
			i_host = MProcFS.register_procimpl ("host", this);
		}
		//}}}
		//{{{  public String readproc (MInode inode, String name)
		/**
		 * called to read something out via proc-fs
		 */
		public String readproc (MInode inode, String name)
		{
			String r = "";

			if (inode == i_version) {
				r = "MOSS " + MConfig.version + "\n";
			} else if (inode == i_host) {
				r = r + "host system: " + System.getProperty ("os.name") + "/" + System.getProperty ("os.arch") +
					" ver " + System.getProperty ("os.version") + "\n";
				r = r + "runtime environment: " + System.getProperty ("java.version") +
					"/" + System.getProperty ("java.vendor") + "\n";
				r = r + "virtual system: " + System.getProperty ("java.vm.vendor") +
					"  " + System.getProperty ("java.vm.name") + "/" + System.getProperty ("java.vm.version") + "\n";

			}
			return r;
		}
		//}}}
	}
	//}}}

	//{{{  public static void init_kernel (MProcessor cpus[], PrintStream msgs)
	/**
	 * initialises the kernel
	 *
	 * @param cpus array of virtual processors to use
	 * @param msgs somewhere we can write boot-messages
	 */
	public static void init_kernel (MProcessor cpus[], PrintStream msgs)
	{
		//Set this value to make the specific scheduler active <manj>
		m_schedularType = SchedulerType.FIFO;
		
		m_schedular = NewScheduler();
		
		current = new MProcess[MConfig.ncpus];
		lock = new CREWLock ();
		nextpid = 0;

		msgs.println ("MKernel starting...");
		init_task = null;
		task_list = null;
		processors = cpus;

		if (processors.length != MConfig.ncpus) {
			panic ("MKernel::init_kernel() processor number problem");
		}

		/* print some information about the host environment */
		msgs.println ("host system: " + System.getProperty ("os.name") + "/" + 
				System.getProperty ("os.arch") + " ver " + System.getProperty ("os.version"));
		msgs.println ("runtime environment: " + System.getProperty ("java.version") + 
				"/" + System.getProperty ("java.vendor"));
		msgs.println ("virtual system: " + System.getProperty ("java.vm.vendor") + 
				"  " + System.getProperty ("java.vm.name") + "/" + System.getProperty ("java.vm.version"));

		/* CPU information */
		for (int i=0; i<processors.length; i++) {
			msgs.println ("CPU" + i + " is a " + processors[i].get_processor_id());
		}

		MNamedMsgQ.init_namedmsgq ();
		MLog.init_log ();
		MModules.init_modules ();
		MTimer.init_timer ();
		MDevices.init_devices ();
		MMailBox.init_mailbox ();
		MSemaphore.init_semaphore ();
		MFileSystem.init_filesystem ();
	}
	//}}}
	//{{{  public static void panic (String message)
	/**
	 * causes a kernel panic; that aborts the simulator
	 *
	 * @param message informational message
	 */
	public static void panic (String message)
	{
		System.out.println ("\n\nMOSS: panic! " + message);
		System.out.println ("System halted.");
		System.out.flush ();
		throw new RuntimeException ("PANIC!");
		// System.exit (0);
	}
	//}}}
	//{{{  public static void log_msg (String msg)
	/**
	 * generates a kernel `log' message
	 *
	 * @param msg log string (without terminating newline)
	 */
	public static void log_msg (String msg)
	{
		MLog.writelog (msg);
		return;
	}
	//}}}
	//{{{  public static void schedule ()
	/**
	 * this deschedules the current process, picks a new process from the run-queue and runs it.
	 */
	public static void schedule ()
	{
		getScheduler().Schedule();
	}
	//}}}
	//{{{  private static void schedule_to_cpu (MProcess p, int cpu)
	/**
	 * This is used to set a process running on a particular processor.  Processor must be idle..
	 * Also, the kernel lock must be held in here.
	 */
	private static void schedule_to_cpu (MProcess p, int cpu)
	{
		if (!processors[cpu].is_idle()) {
			panic ("MKernel::schedule_to_cpu() active processor!");
		}
		// System.err.println ("schedule_to_cpu(" + p.getName() + ", " + cpu + ")!");
		processors[cpu].set_process (p);
		/* wake up the sleeping process */
		synchronized (p) {
			p.notify ();
		}
	}
	//}}}
	//{{{  public static void first_process (MProcess p)
	/**
	 * this is called once to set the first process up.
	 * It gets PID 1.
	 *
	 * @param p an INVALID MProcess (not started as a Thread, yet)
	 */
	public static void first_process (MProcess p)
	{
		lock.claim_write ();
		init_task = p;
		current[0] = init_task;
		processors[0].set_process (init_task);
		init_task.state = MProcess.TASK_RUNNING;
		init_task.pid = 1;
		nextpid = 2;
		lock.release_write ();
		init_task.start ();
	}
	//}}}
	//{{{  public static void finalinit ()
	/**
	 * this is called by the init-task as the first thing it does;  final kernel
	 * setup happens here, and we're in the context of the init-process.
	 */
	public static void finalinit ()
	{
		MLog.finalinit ();
		MProcessor.finalinit ();
		MDevices.finalinit ();
		MMailBox.finalinit ();
		MFileSystem.finalinit ();
		MProcFS.finalinit ();
		MModules.finalinit ();
		MNamedMsgQ.finalinit ();
		MSemaphore.finalinit ();

		new PFS_mkernel ();

		return;
	}
	//}}}
	//{{{  public static void starting_process (MProcess p)
	/**
	 * this is invoked by a process as it starts up -- not "running" as far as MOSS is concerned.
	 * When this returns, it is as a properly running MOSS process.
	 *
	 * The other half of this is performed by start_process
	 *
	 * @param p process that is starting
	 */
	public static void starting_process (MProcess p)
	{
		int cpu;

		/* add it to the run-queue */
		quiet_add_to_run_queue (p);

		synchronized (p) {
			/* notify process start semaphore */
			p.start_sem.sem_signal ();

			/* go to sleep */
			try {
				p.wait ();
			} catch (InterruptedException e) {
				panic ("MKernel::starting_process()  interrupted!");
			}
		}

		/* when a thread wakes up here, it is p */
		cpu = MProcessor.currentCPU ();
		lock.claim_write ();
		current[cpu] = p;
		/* ensure proper detachment from any queue */
		p.state = MProcess.TASK_RUNNING;
		lock.release_write ();
		// System.err.println ("starting_process(" + current[cpu].getName() + ") on CPU " + cpu);
	}
	//}}}
	//{{{  public static void ending_process (MProcess p, int exitcode)
	/**
	 * this is invoked by a process as it finishes.
	 */
	public static void ending_process (MProcess p, int exitcode)
	{
		/* we enter this with the "current" Thread */
		MProcess old_p, new_p;
		int cpu = MProcessor.currentCPU ();

		lock.claim_write ();

		if (current[cpu] == null) {
			panic ("MKernel::ending_process().  current[cpu] is null!");
		}

		/* reparent any child processes (init inherits) */
		for (MProcess x = task_list; x != null; x = x.next_task) {
			if (x.parent == p) {
				/* this one */
				x.parent = init_task;
			}
		}
		lock.release_write ();

		/* tell parent process -- done before we check the run-queue. */
		if (current[cpu].parent != null) {
			MSignal chldsig = new MSignal (MSignal.SIGCHLD, (Object)(new int[] {current[cpu].pid, exitcode}));
			
			queue_signal (current[cpu].parent, chldsig);
		}

		lock.claim_write ();

		if (!getScheduler().IsProcessAvailable()) {
			/* nothing else to run, make processor idle */
			old_p = current[cpu];
			new_p = null;
			processors[cpu].set_process (null);
		} else {
			/* pick a process off the run-queue */
			old_p = current[cpu];
			new_p = getScheduler().GetNextProcess();
			
			processors[cpu].set_process (new_p);
		}
		current[cpu] = null;			/* just incase anything tries during the reschedule */

		lock.release_write ();

		if (new_p != null) {
			synchronized (new_p) {
				new_p.notify ();
			}
		}

		/* and we're done..! */
	}
	//}}}
	//{{{  public static void start_process (MProcess p)
	/**
	 * this is called to start a new process.  The process given should be
	 * a non-started MProcess (extending Java's Thread).
	 *
	 * @param p process to be started
	 */
	public static void start_process (MProcess p)
	{
		if (p.isAlive ()) {
			panic ("MKernel::start_process() process [" + p.getName() + "] already running!");
		}
		p.start ();
		/* wait for it */
		p.start_sem.sem_wait ();
	}
	//}}}
	//{{{  public static int get_free_pid ()
	/**
	 * returns a free PID (and stops it being allocated again until released)
	 */
	public static int get_free_pid ()
	{
		return nextpid++;
	}
	//}}}
	//{{{  public static void release_free_pid (int pid)
	/**
	 * returns a used PID to the system
	 *
	 * @param pid process-ID no longer in use
	 */
	public static void release_free_pid (int pid)
	{
		/* nothing yet, pids are allocated incrementally forever.. */
	}
	//}}}
	//{{{  public static void quiet_add_to_run_queue (MProcess p)
	/**
	 * adds a process to the run-queue.  Does not attempt to schedule process.
	 */
	public static void quiet_add_to_run_queue (MProcess p)
	{
		p.state = MProcess.TASK_RUNNABLE;
		lock.claim_write ();
		getScheduler().AddProcess(p);
		lock.release_write ();
	}
	//}}}
	//{{{  public static void add_to_run_queue (MProcess p)
	/**
	 * adds a process to the run-queue.  If its not already running, and there
	 * is a free-processor, it is dispatched immediately.
	 *
	 * @param p process to add.  Must not be on any other queue!
	 */
	public static void add_to_run_queue (MProcess p)
	{
		p.state = MProcess.TASK_RUNNABLE;
		lock.claim_write ();
		if (!getScheduler().IsProcessAvailable()) {
			int freecpu = -1;

			/* see if there's a spare processor (and the process isn't already running) */
			for (int i=0; i<processors.length; i++) {
				if (processors[i].is_idle()) {
					/* got an idle CPU, check running */
					freecpu = i;

					for (int j=0; j<current.length; j++) {
						if (current[j] == p) {
							/* already running (rare, but can happen) */
							freecpu = -1;
							break;
						}
					}
					break;
				}
			}
			if (freecpu >= 0) {
				/* have a free processor, schedule process */
				schedule_to_cpu (p, freecpu);
			} else {
				/* make it the run-queue */
				getScheduler().AddProcess(p);
			}
		} else {
			getScheduler().AddProcess(p);
		}
		lock.release_write ();
	}
	//}}}
	//{{{  public static void add_to_task_list (MProcess p)
	/**
	 * adds a process to the global task list.  Should only be used when creating a new process.
	 *
	 * @param p process to add
	 */
	public static void add_to_task_list (MProcess p)
	{
		lock.claim_write ();
		p.next_task = task_list;
		p.prev_task = null;
		if (task_list != null) {
			task_list.prev_task = p;
		}
		task_list = p;
		lock.release_write ();
	}
	//}}}
	//{{{  public static void remove_from_task_list (MProcess p)
	/**
	 * removes a process from the global task queue.  Should only be used when destroying a process.
	 *
	 * @param p process to remove
	 */
	public static void remove_from_task_list (MProcess p)
	{
		MProcess t;

		lock.claim_write ();
		/* search, just to make sure it's here first.. */
		for (t = task_list; (t != null) && (t != p); t = t.next_task);
		if (t == null) {
			panic ("MKernel::remove_from_task_list() no such task.");
		}
		if (p.prev_task == null) {
			task_list = p.next_task;
			task_list.prev_task = null;
		} else if (p.next_task == null) {
			p.prev_task.next_task = null;
		} else {
			p.prev_task.next_task = p.next_task;
			p.next_task.prev_task = p.prev_task;
		}
		lock.release_write ();
	}
	//}}}
	//{{{  public static MProcess find_process (int pid)
	/**
	 * finds a process from its PID
	 *
	 * @param pid PID of process to search for
	 *
	 * @return MProcess reference on success, or null if not found
	 */
	public static MProcess find_process (int pid)
	{
		MProcess tmp;

		lock.claim_read ();
		for (tmp=task_list; tmp != null; tmp = tmp.next_task) {
			if (tmp.pid == pid) {
				break;		/* from for() */
			}
		}
		lock.release_read ();
		
		return tmp;
	}
	//}}}
	//{{{  public static void deliver_process_signals (MProcess p)
	/**
	 * this delivers any pending signals to process p.
	 * This (p) *must* be the/a current process..
	 *
	 * @param p a current process
	 */
	public static void deliver_process_signals (MProcess p)
	{
		boolean kill_process = false;
		MSignal sig = null;
		MSignal sigq;

		synchronized (p) {
			if (!p.signalled && (p.pending_signals == null)) {
				/* no signals */
				return;
			} else if (!p.signalled || (p.pending_signals == null)) {
				/* shouldn't happen */
				panic ("MKernel::deliver_process_signals() - bad signal state..");
			}
			/* process signals */
			p.signalled = false;
			sigq = p.pending_signals;
			p.pending_signals = null;
		}

		while (sigq != null) {
			sig = sigq;

			sigq = sig.next;
			sig.next = null;
			switch (p.sig_handling[sig.signo]) {
			case MSignal.SIG_IGN:
				/* easy -- ignore signal! :) */
				break;
			case MSignal.SIG_DFL:
				/* default action, most are terminate process, few specials */
				switch (sig.signo) {
				case MSignal.SIGSTOP:
					/* stop process.. */
					// set state and deschedule.
					p.state = MProcess.TASK_STOPPED;
					schedule ();
					// panic ("MKernel::deliver_process_signals() - FIXME: SIGSTOP");
					break;
				case MSignal.SIGCHLD:
					/* ignore this */
					break;
				default:
					/* terminate process -- this is similar to what MPosixIf.exit(int) does */
					kill_process = true;
					sigq = null;			/* break the while() loop */
					break;
				}
				break;
			case MSignal.SIG_CATCH:
				/* handled by the application */
				try {
					p.user_if.signal (sig.signo, sig.sigdata);
				} catch (RuntimeException e) {
					process_fault (p, e);
					kill_process = true;
					sigq = null;			/* break the while() loop */
					break;
				}
				break;
			}
		}
		if (kill_process) {
			MProcess.shutdown_process (p);
			ending_process (p, 128 + sig.signo);
			remove_from_task_list (p);
			/* destroy thread */
			//p.destroy ();
			/* and deadlock if we're still here */
			synchronized (p) {
				try {
					p.wait ();
				} catch (InterruptedException e) {}
			}
		}
	}
	//}}}
	//{{{  public static void queue_signal (MProcess p, MSignal signal)
	/**
	 * queues a signal to be delivered to the given process
	 *
	 * @param p process to be signalled
	 * @param signal signal to be delivered
	 */
	public static void queue_signal (MProcess p, MSignal signal)
	{
		boolean schedp = false;

		if ((p == null) || (p.ktask == true)) {
			return;
		}
		synchronized (p) {
			switch (signal.signo) {
			case MSignal.SIGKILL:
			case MSignal.SIGSTOP:
				/* can't catch either of these! */
				break;
			default:
				if (p.sig_handling[signal.signo] != MSignal.SIG_IGN) {
					signal.next = p.pending_signals;
					p.pending_signals = signal;
					p.signalled = true;
					schedp = true;
				}
				/* else don't bother delivering */
			}

			switch (signal.signo) {
			case MSignal.SIGCONT:
				if (schedp && (p.state == MProcess.TASK_STOPPED)) {
					/* resume stopped process (easy!) */
					p.state = MProcess.TASK_SLEEPING;
				}
				break;
			case MSignal.SIGSTOP:
				break;
			}
		}

		if (schedp && (p.state == MProcess.TASK_SLEEPING)) {
			/* schedule it for execution, whereever it's sleeping *must* check
			 * if it woke because of a signal (signalled == true)
			 */
			add_to_run_queue (p);
		}
	}
	//}}}
	//{{{  public static MProcess[] get_process_list ()
	/**
	 * extracts a process list.  This returns an array of MProcess's, that are
	 * semi-populated copies of the real ones.  There must be at least one..!
	 *
	 * @return array of process "control blocks"
	 */
	public static MProcess[] get_process_list ()
	{
		int nprocs;
		MProcess procdata[];

		lock.claim_read ();
		/* count number of processes first */
		nprocs = 0;
		for (MProcess tmp = task_list; tmp != null; tmp = tmp.next_task) {
			nprocs++;
		}
		procdata = new MProcess[nprocs];
		nprocs = 0;
		for (MProcess tmp = task_list; tmp != null; tmp = tmp.next_task) {
			procdata[nprocs] = new MProcess();
			procdata[nprocs].state = tmp.state;
			procdata[nprocs].signalled = tmp.signalled;
			procdata[nprocs].ktask = tmp.ktask;
			procdata[nprocs].pid = tmp.pid;
			procdata[nprocs].cmdline = (String[])(tmp.cmdline.clone());
			procdata[nprocs].setName (tmp.getName());
			procdata[nprocs].syscall = (tmp.syscall == null) ? null : new String(tmp.syscall);
			procdata[nprocs].ppid = (tmp.parent == null) ? 0 : tmp.parent.pid;
			nprocs++;
		}
		lock.release_read ();
		return procdata;
	}
	
	public static Boolean setProcessPriority(int pid, ProcessPriorityEnum priority) {
		Boolean retValue = false;
		
		lock.claim_read();
		
		//Check if process is currently running
		for (int i=0; i<current.length; i++) {
			MProcess iterateProcess = current[i];
			if (iterateProcess == null)
				continue;
			
			if (iterateProcess.pid == pid) {
				if (iterateProcess instanceof IPriorityProcess) {
					((IPriorityProcess)iterateProcess).SetPriority(priority);
				}
				else {
					//Pid found, however the process doen't support Priority mechanism.
					retValue = false;
					break;
				}
			}
		}
		
		//Check within scheduler list.
		if (getScheduler() instanceof IPriorityScheduler){
			retValue = ((IPriorityScheduler)getScheduler()).setProcessPriority(pid, priority);
		}
		else {
			retValue = false;
		}
		
		lock.release_read();
		
		return retValue;
	}
	//}}}
	//{{{  public static void process_fault (MProcess p, RuntimeException e)
	/**
	 * called when a process generates a run-time error (typically caught
	 * RuntimeExceptions for main-code or signal-handling).
	 *
	 * @param p process that faulted
	 * @param e Java RuntimeException generated
	 */
	public static void process_fault (MProcess p, RuntimeException e)
	{
		StackTraceElement st[];
		int i;

		st = e.getStackTrace();
		if (st != null) {
			log_msg ("MKernel::process_fault(): " + p.pid + " (" + p.getName() + ") [fault: " + e + "] at:");
			for (i=0; i<st.length; i++) {
				log_msg ("    " + st[i].toString());
			}
		}
		if (p == init_task) {
			panic ("MKernel::process_fault() -- init task died!");
		}
		return;
	}
	//}}}
	//{{{  public static void module_fault (MProcess p, RuntimeException e)
	/**
	 * called when a kernel module generates a run-time error
	 *
	 * @param p process that faulted
	 * @param e Java RuntimeException generated
	 */
	public static void module_fault (MProcess p, RuntimeException e)
	{
		StackTraceElement st[];
		int i;

		st = e.getStackTrace();
		if (st != null) {
			log_msg ("MKernel::module_fault(): " + p.pid + " (" + p.getName() + ") [fault: " + e + "] at:");
			for (i=0; i<st.length; i++) {
				log_msg ("    " + st[i].toString());
			}
		}
		return;
	}
	//}}}
	
	
	public static MProcess NewProcess(MProcess parentProcess)
	{
		switch(m_schedularType)
		{
		case FIFO:
			return new MProcess(parentProcess);
		case Lottery:
			return new MLotteryProcess(parentProcess);
		case PrioritizedLottery:
			return new MPrioritizedLotteryProcess(parentProcess);
		case PrioritizedRoundRobbin:
			return new MPrioritizedProcess(parentProcess);
		}
		
		return null;
	}
	
	private static IScheduler NewScheduler()
	{
		switch(m_schedularType)
		{
		case FIFO:
			return new FIFOScheduler();
		case Lottery:
			return new LotteryScheduler();
		case PrioritizedLottery:
			return new PrioritizedLotteryScheduler();
		case PrioritizedRoundRobbin:
			return new RoundRobinPriorityScheduler();
		}
		
		return null;
	}
}

