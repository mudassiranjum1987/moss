/*
 *	Semaphore.java -- semaphore implementation
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
 * This class provides a low-level semaphore.  Unlike MSemaphore(), this
 * will "hold up" a virtual-processor if it waits.
 */

public class Semaphore
{
	//{{{  private data
	private int v;
	//}}}

	//{{{  public Semaphore ()
	/**
	 * creates and initialises a semaphore (default is to 1, for a mutex)
	 */
	public Semaphore ()
	{
		this (1);
	}
	//}}}
	//{{{  public Semaphore (int value)
	/**
	 * creates and initialises a semaphore to the given value
	 *
	 * @param value initial semaphore value
	 */
	public Semaphore (int value)
	{
		v = value;
	}
	//}}}
	//{{{  public synchronized boolean sem_wait ()
	/**
	 * wait on this semaphore
	 *
	 * @return true if the wait completed successfully, false if the (Java) thread was interrupted whilst waiting
	 */
	public synchronized boolean sem_wait ()
	{
		v--;
		if (v < 0) {
			try {
				wait ();
				return true;
			} catch (InterruptedException e) {
				return false;
			}
		}
		return true;
	}
	//}}}
	//{{{  public synchronized void sem_signal ()
	/**
	 * signal this semaphore
	 */
	public synchronized void sem_signal ()
	{
		v++;
		notify ();
	}
	//}}}
}

