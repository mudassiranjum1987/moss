/*
 *	MMailBox.java -- mail-box style message passing for MOSS
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

import java.util.*;

/**
 * this class provides mailbox style IPC.  processes implicitly get mailboxes
 * addressed by their PID.
 */

public class MMailBox
{
	//{{{  local variables/classes
	/** mutex for variables */
	private static Object synclock;

	/** message hashtable (keys are process IDs) */
	private static Hashtable msgs;

	/** private class to define a "message queue" */
	private static class MBoxQueue
	{
		int tpid;			/* target PID */
		public ArrayList msgs;
		public MWaitQueue b_recv;

		public MBoxQueue (int tpid)
		{
			this.tpid = tpid;
			this.msgs = new ArrayList ();
			this.b_recv = new MWaitQueue ();
			return;
		}
	}

	/** private class to define a "message" */
	private static class MBox
	{
		public int from_pid;
		public int to_pid;
		public int type;
		public Object message;
	}


	/** this class provides "mailboxes" in the process file-system */
	private static class PFS_mailboxes implements MProcFSIf
	{
		public String readproc (MInode inode, String name)
		{
			String r = "";
			int i;

			return "hello from mailboxes!\n";

		}
	}

	//}}}
	//{{{  private static MBoxQueue find_mailbox (int pid)
	/**
	 * looks a mailbox queue for a particular process, creates it if it does
	 * not already exist.
	 *
	 * @param pid process ID
	 * 
	 * @return mailbox queue for the process
	 */
	private static MBoxQueue find_mailbox (int pid)
	{
		Object o;
		MBoxQueue mbq;

		synchronized (synclock) {
			o = msgs.get (new Integer (pid));
			if (o == null) {
				mbq = new MBoxQueue (pid);
				msgs.put (new Integer (pid), mbq);
			} else {
				mbq = (MBoxQueue)o;
			}
		}

		return mbq;
	}
	//}}}


	//{{{  public static void init_mailbox ()
	/**
	 * initialises the MMailBox class -- should be called at system startup
	 */
	public static void init_mailbox ()
	{
		msgs = new Hashtable ();
		synclock = new Object ();
		return;
	}
	//}}}
	//{{{  public static void finalinit ()
	/**
	 * called to do final initialisation, in the context of the init-task
	 */
	public static void finalinit ()
	{
		MProcFS.register_procimpl ("mailboxes", new PFS_mailboxes());

		return;
	}
	//}}}


	//{{{  public static int sendmsg (int source, int dest, int type, Object msg)
	/**
	 * called to send a message.  This is always asynchronous -- i.e. this method
	 * will not block.  It also provides a way for asynchronous Java threads
	 * (e.g. for graphics) to deliver messages to some MOSS process.
	 *
	 * @param source PID of source process (i.e. invoking process)
	 * @param dest PID of destination process
	 * @param type message type (application specific)
	 * @param msg object message (application specific)
	 *
	 * @return 0 on success, otherwise -ve indicating error
	 */
	public static int sendmsg (int source, int dest, int type, Object msg)
	{
		MBox mbox = new MBox ();
		MBoxQueue mbq;

		mbox.from_pid = source;
		mbox.to_pid = dest;
		mbox.type = type;
		mbox.message = msg;

		mbq = find_mailbox (dest);
		
		/* add mbox message to the mailbox queue and wake up any blocked receivers */
		synchronized (mbq) {
			mbq.msgs.add (mbox);
			while (!mbq.b_recv.is_empty()) {
				MProcess p = mbq.b_recv.get_from_queue ();

				MKernel.add_to_run_queue (p);
			}
		}

		return 0;
	}
	//}}}
	//{{{  public static Object recvmsg (int source, int dest, int type)
	/**
	 * called to receive a message.  If no messages are available, the process will
	 * be descheduled until one becomes available.
	 *
	 * @param source PID of source process (or -1 for any)
	 * @param dest PID of receiving process (i.e. invoking process)
	 * @param type message type (application specific, or -1 for any)
	 *
	 * @return the object message or null if signalled
	 */
	public static Object recvmsg (int source, int dest, int type)
	{
		boolean looping = true;
		Object msg = null;
		MBoxQueue mbq;

		mbq = find_mailbox (dest);

		while (looping) {
			/* lock structures and scan for messages */
			synchronized (mbq) {
				int i;

				for (i = 0; looping && (i < mbq.msgs.size()); i++) {
					MBox mbox = (MBox)mbq.msgs.get (i);

					if ((mbox.to_pid == dest) &&
							((source == -1) || (source == mbox.from_pid)) &&
							((type == -1) || (type == mbox.type))) {
						/* found one! */
						looping = false;
						msg = mbox.message;
						mbq.msgs.remove (i);
					}
				}
			}
			if (msg == null) {
				MProcess current = MKernel.current[MProcessor.currentCPU()];
				boolean do_sleep = true;

				/* no message found, put process to sleep */
				if (current.signalled) {
					do_sleep = false;
				} else {
					synchronized (mbq) {
						mbq.b_recv.add_to_queue (current);
						synchronized (current) {
							current.state = MProcess.TASK_SLEEPING;
						}
					}
					synchronized (current) {
						do_sleep = ((current.state == MProcess.TASK_SLEEPING) && !current.signalled);
					}
				}
				if (do_sleep) {
					MKernel.schedule ();
					if (current.signalled) {
						synchronized (mbq) {
							mbq.b_recv.del_from_queue (current);
						}
						looping = false;
						msg = null;
					}
				}
			}
		}
		return msg;
	}
	//}}}
	//{{{  public static void deadprocess (int pid)
	/**
	 * this is called by the kernel when a process exits.  any messages queued
	 * for it are removed.
	 *
	 * @param pid terminating process ID
	 */
	public static void deadprocess (int pid)
	{
		Object o = msgs.get (new Integer (pid));

		if (o == null) {
			return;
		}
		msgs.remove (new Integer (pid));
		return;
	}
	//}}}
}


