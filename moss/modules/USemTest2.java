/*
 *	USemTest2.java -- semaphore test (waiter)
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

package moss.modules;

import moss.user.*;
import moss.ipc.MSemaphore;


public class USemTest2 implements MUserProcess
{
	public int main (String argv[], MEnv envp)
	{
		int semkey = -1;
		int waitcount = 0;
		int x;
		byte buffer[];
		

		if (argv.length < 3) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": um, incorrect usage!\n");
			MPosixIf.exit (1);
		}
		try {
			semkey = Integer.parseInt (argv[1]);
			waitcount = Integer.parseInt (argv[2]);
		} catch (NumberFormatException e) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": bad argument.\n");
			MPosixIf.exit (1);
		}

		/* say hello */
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + " -- semaphore test (waiter on " + semkey + ", for " + waitcount + ")\n");

		/* do semaphore wait */
		x = MPosixIf.semop (MSemaphore.SEMOP_SET, semkey, -waitcount);
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": semop() returned: " + x + "\n");
			MPosixIf.exit (1);
		}
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": yay, " + waitcount + " out!\n"); 
		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDOUT, "USemTest2 signalled with " + signo + "!\n");
	}

}

