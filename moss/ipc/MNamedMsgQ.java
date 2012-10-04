/*
 *	MNamedMsgQ.java -- named message queues for MOSS
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

import java.util.*;

/**
 * this class provides named message-queues.  it allows kernel and user processes
 * to exchange messages, broadcast messages, etc.
 */

public class MNamedMsgQ
{
	//{{{  local variables/classes
	
	//{{{  private static class PMQProcCxt {
	/**
	 * this class provides a message-queue process context -- messages for processes collect here
	 */
	private static class PMQProcCxt {
		MProcess p;
		LinkedList msgs;
		MWaitQueue waiting;
	}
	//}}}
	//{{{  private static class PrivMsgQ
	/**
	 * this class looks after a single named message queue
	 */
	private static class PrivMsgQ implements MFileOps
	{
		public String name;
		public int nhandles;
		public ArrayList pcxts;

		//{{{  public int open (MFile handle, int flags)
		/**
		 * called when the queue is opened (only called once for each handle)
		 */
		public int open (MFile handle, int flags)
		{
			MProcess current = MKernel.current[MProcessor.currentCPU()];
			PMQProcCxt pcxt = new PMQProcCxt ();

			pcxt.p = current;
			pcxt.msgs = new LinkedList ();
			pcxt.waiting = new MWaitQueue ();

			synchronized (this) {
				pcxts.add (pcxt);
			}

			handle.pdata = (Object)pcxt;

			return 0;
		}
		//}}}
		//{{{  public int close (MFile handle)
		/**
		 * called when the queue is closed (only called once for each handle)
		 */
		public int close (MFile handle)
		{
			int i;
			PMQProcCxt pcxt = (PMQProcCxt)(handle.pdata);

			/* remove the process context */
			synchronized (this) {
				i = pcxts.indexOf (pcxt);
				if (i >= 0) {
					pcxts.remove (i);
				}
			}
			handle.pdata = null;
			if (i < 0) {
				MKernel.log_msg ("MNamedMsgQ.PrivMsgQ.close(): process context not here..");
				return -MSystem.EIO;
			}

			return MNamedMsgQ.nmq_close (handle, name);
		}
		//}}}
		//{{{  public int lseek (MFile handle, int offset, int whence)
		/**
		 * seek on the queue (invalid)
		 */
		public int lseek (MFile handle, int offset, int whence)
		{
			return -MSystem.ESPIPE;
		}
		//}}}
		//{{{  public int read (MFile handle, byte buffer[], int count)
		/**
		 * read from the queue (invalid)
		 */
		public int read (MFile handle, byte buffer[], int count)
		{
			return -MSystem.EIO;
		}
		//}}}
		//{{{  public int write (MFile handle, byte buffer[], int count)
		/**
		 * write to the queue (invalid)
		 */
		public int write (MFile handle, byte buffer[], int count)
		{
			return -MSystem.EIO;
		}
		//}}}
		//{{{  public int fcntl (MFile handle, int op, int arg)
		/**
		 * fcntl on the queue (invalid)
		 */
		public int fcntl (MFile handle, int op, int arg)
		{
			return -MSystem.EIO;
		}
		//}}}
	}
	//}}}
	
	/** this acts as the lock for message-queue data */
	private static Object synclock;

	/** this is the named message queue, held in both an ArrayList and Hashtable (hashed on the name) */
	private static ArrayList a_msgs;
	private static Hashtable h_msgs;


	//}}}
	//{{{  public classes
	/**
	 * this class is used to return data in a nmq_read() call
	 */
	public static class MNamedMsg
	{
		public Object msg;
		public int type;
	}
	//}}}

	//{{{  public static void init_namedmsgq ()
	/**
	 * called to initialise
	 */
	public static void init_namedmsgq ()
	{
		synclock = new Object ();

		a_msgs = new ArrayList ();
		h_msgs = new Hashtable ();

		return;
	}
	//}}}
	//{{{  public static void finalinit ()
	/**
	 * performs final initialisation (in context of the init-task)
	 */
	public static void finalinit ()
	{
	}
	//}}}
	//{{{  public static void shutdown_namedmsgq ()
	/**
	 * called to shut-down
	 */
	public static void shutdown_namedmsgq ()
	{
		return;
	}
	//}}}

	//{{{  public static int nmq_open (MFile handle, String name)
	/**
	 * opens a named message queue -- creates the queue if it doesn't exist
	 *
	 * @param handle file-handle to be associated with this queue
	 * @param name name of the message queue to create
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int nmq_open (MFile handle, String name)
	{
		Object o;
		PrivMsgQ pmq = null;

		synchronized (synclock) {
			o = h_msgs.get (name);
			if (o == null) {
				/* create it */
				pmq = new PrivMsgQ ();
				pmq.name = new String (name);
				pmq.nhandles = 0;
				pmq.pcxts = new ArrayList ();
				/* and add to the list */
				h_msgs.put (name, pmq);
				a_msgs.add (pmq);
			} else {
				pmq = (PrivMsgQ)o;
			}

			pmq.nhandles++;
		}

		handle.pdata = null;		/* gets set by open() in PrivMsgQ */
		handle.fileif = pmq;

		pmq.open (handle, 0);

		return 0;
	}
	//}}}
	//{{{  public static int nmq_close (MFile handle, String name)
	/**
	 * closes a named message queue -- this is called locally by PrivMsgQ.close()
	 *
	 * @param handle file-handle associated with this queue (one of them)
	 * @param name name of this message queue
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int nmq_close (MFile handle, String name)
	{
		Object o;
		PrivMsgQ pmq;

		synchronized (synclock) {
			o = h_msgs.get (name);
			if (o == null) {
				return -MSystem.ENOENT;
			}
			pmq = (PrivMsgQ)o;

			pmq.nhandles--;
			if (pmq.nhandles == 0) {
				/* last process on this queue, remove it */
				int idx = a_msgs.indexOf (pmq);

				if (idx < 0) {
					MKernel.panic ("MNamedMsgQ::nmq_close(): queue not in list!");
				}
				h_msgs.remove (name);
				a_msgs.remove (idx);
			}
		}
		handle.fileif = null;
		handle.pdata = null;

		return 0;
	}
	//}}}
	//{{{  public static int nmq_send (MFile handle, int type, Object msg)
	/**
	 * sends a message to the named message queue
	 *
	 * @param handle handle on the message-queue
	 * @param type application defined type field
	 * @param msg object message
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int nmq_send (MFile handle, int type, Object msg)
	{
		PrivMsgQ pmq;
		MNamedMsg nmsg;
		PMQProcCxt pcxt;

		try {
			pmq = (PrivMsgQ)(handle.fileif);
			pcxt = (PMQProcCxt)(handle.pdata);
		} catch (ClassCastException e) {
			return -MSystem.EBADF;
		}

		nmsg = new MNamedMsg ();
		nmsg.msg = msg;
		nmsg.type = type;

		synchronized (pmq) {
			int i;

			for (i=0; i<pmq.pcxts.size (); i++) {
				PMQProcCxt px = (PMQProcCxt)(pmq.pcxts.get (i));

				if (px != pcxt) {
					/* not us, so add message */
					px.msgs.add (nmsg);
					if (!px.waiting.is_empty()) {
						/* wake up a blocked receiver */
						MProcess p = px.waiting.get_from_queue ();

						MKernel.add_to_run_queue (p);
					}
				}
			}
		}

		return 0;
	}
	//}}}
	//{{{  public static int nmq_recv (MFile handle, MNamedMsg msg)
	/**
	 * waits for a message from the named message queue
	 *
	 * @param handle handle on the message-queue
	 * @param msg where the message is stored
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int nmq_recv (MFile handle, MNamedMsg msg)
	{
		PrivMsgQ pmq;
		PMQProcCxt pcxt;
		boolean do_sleep = false;
		MProcess current = MKernel.current[MProcessor.currentCPU()];

		try {
			pmq = (PrivMsgQ)(handle.fileif);
			pcxt = (PMQProcCxt)(handle.pdata);
		} catch (ClassCastException e) {
			return -MSystem.EBADF;
		}

		for (;;) {
			boolean xsleep;

			synchronized (pmq) {
				if (pcxt.msgs.size() > 0) {
					/* can get something */
					MNamedMsg xmsg = (MNamedMsg)(pcxt.msgs.removeFirst());

					msg.msg = xmsg.msg;
					msg.type = xmsg.type;
					break;				/* for(;;) */
				} else {
					/* nothing here, put on queue and sleep */
					pcxt.waiting.add_to_queue (current);
					current.state = MProcess.TASK_SLEEPING;
					do_sleep = true;
				}
			}

			/* committed to sleeping */
			synchronized (current) {
				xsleep = ((current.state == MProcess.TASK_SLEEPING) && !current.signalled);
			}
			if (xsleep) {
				/* reschedule */
				MKernel.schedule ();
			}
			if (current.signalled) {
				/* interrupted -- remove from wait queue */
				synchronized (pmq) {
					pcxt.waiting.del_from_queue (current);
				}
				return -MSystem.EINTR;
			}
			/* woke up, go round for another go */
		}

		return 0;
	}
	//}}}
}



