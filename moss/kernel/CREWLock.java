/*
 *	CREWLock.java -- CREW (concurrent read, exclusive write) style lock
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

/* Note: this is based on code from Peter Welch's and David Wood's occam CREW lock */

/**
 * implementation of a CREW (concurrent read, exclusive write) lock for MOSS processes.
 * Based on Peter Welch's and David Wood's CREW code from occam/KRoC.  This is
 * a bit simpler than the algorithm presented in the lecture.
 */

public class CREWLock
{
	//{{{  private data
	private Semaphore notify;
	private int nreaders;
	private Semaphore nreaders_sem;
	private MProcess waiting_writer;
	//}}}

	//{{{  public CREWLock ()
	/**
	 * creates and initialises a new CREW lock
	 */
	public CREWLock ()
	{
		/* initialise CREW lock */
		nreaders = 0;
		notify = new Semaphore (1);
		nreaders_sem = new Semaphore (1);
		waiting_writer = null;
	}
	//}}}
	//{{{  public void claim_read ()
	/**
	 * claim a read lock
	 */
	public void claim_read ()
	{
		notify.sem_wait ();
		nreaders_sem.sem_wait ();
		nreaders++;
		nreaders_sem.sem_signal ();
		notify.sem_signal ();
	}
	//}}}
	//{{{  public void release_read ()
	/**
	 * release a read lock
	 */
	public void release_read ()
	{
		nreaders_sem.sem_wait ();
		nreaders--;
		if ((nreaders == 0) && (waiting_writer != null)) {
			/* wake-up waiting writer */
			MKernel.add_to_run_queue (waiting_writer);
			waiting_writer = null;
		}
		nreaders_sem.sem_signal ();
	}
	//}}}
	//{{{  public void claim_write ()
	/**
	 * claim the write lock
	 */
	public void claim_write ()
	{
		notify.sem_wait ();
		nreaders_sem.sem_wait ();
		if (nreaders > 0) {
			int cpu = MProcessor.currentCPU ();

			waiting_writer = MKernel.current[cpu];
			MKernel.current[cpu].state = MProcess.TASK_SLEEPING;
			nreaders_sem.sem_signal ();
			MKernel.schedule ();
		} else {
			nreaders_sem.sem_signal ();
		}
	}
	//}}}
	//{{{  public void release_write ()
	/**
	 * release the write lock
	 */
	public void release_write ()
	{
		notify.sem_signal ();
	}
	//}}}
}

