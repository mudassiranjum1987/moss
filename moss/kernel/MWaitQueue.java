/*
 *	MWaitQueue.java -- provides a basic wait-queue
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

/**
 * this class implements a wait-queue.
 *
 * Wait-queues are used for process-context operations, such as keeping
 * track of blocked processes inside a device-driver.
 *
 * This class doesn't do any scheduling itself.
 *
 * It doesn't do any locking itself, either.  Thus, lock outside if
 * necessary.
 */

public class MWaitQueue
{
	private MProcess head;
	private MProcess tail;


	//{{{  public MWaitQueue ()
	/**
	 * constructor -- creates an empty wait-queue
	 */
	public MWaitQueue ()
	{
		head = null;
		tail = null;
	}
	//}}}
	//{{{  public boolean add_to_queue (MProcess p)
	/**
	 * adds a process to the wait-queue
	 *
	 * @param p process to add
	 *
	 * @return true if the process was added, false if not (already there)
	 */
	public boolean add_to_queue (MProcess p)
	{
		if (head == null) {
			/* empty queue */
			head = p;
			tail = p;
			p.q_next = null;
		} else {
			for (MProcess x = head; x != null; x = x.q_next) {
				if (x == p) {
					return false;		/* duplicate */
				}
			}
			tail.q_next = p;
			tail = p;
			p.q_next = null;
		}
		return true;
	}
	//}}}
	//{{{  public MProcess get_from_queue ()
	/**
	 * removes the first process from the wait-queue
	 *
	 * @return process removed, or null if empty
	 */
	public MProcess get_from_queue ()
	{
		MProcess p;

		if (head == null) {
			return null;
		}
		p = head;
		head = p.q_next;
		p.q_next = null;
		return p;
	}
	//}}}
	//{{{  public boolean del_from_queue (MProcess p)
	/**
	 * removes a specific process from the wait-queue
	 *
	 * @param p process to remove
	 * 
	 * @return true if the process was removed, false otherwise (not here)
	 */
	public boolean del_from_queue (MProcess p)
	{
		MProcess prev = null;

		if (head == null) {
			return false;
		}
		for (MProcess x = head; x != null; x = x.q_next) {
			if (x == p) {
				/* remove this one */
				if (p == head) {
					/* first/only in queue */
					if (head == tail) {
						head = null;
						tail = null;
					} else {
						head = head.q_next;
					}
				} else if (p == tail) {
					/* at end of queue */
					tail = prev;
					tail.q_next = null;
				} else {
					/* in the middle somewhere */
					prev.q_next = p.q_next;
				}
				p.q_next = null;
				return true;
			}
			prev = x;
		}
		return false;
	}
	//}}}
	//{{{  public boolean is_empty ()
	/**
	 * tests whether a queue is empty
	 *
	 * @return true if empty, false if waiting processes
	 */
	public boolean is_empty ()
	{
		return (head == null);
	}
	//}}}
}


