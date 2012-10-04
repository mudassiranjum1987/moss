/*
 *	USemTest.java -- simple semaphore implementation (semop) test
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


public class USemTest implements MUserProcess
{
	public int main (String argv[], MEnv envp)
	{
		int x;
		int semkey;
		
		/* say hello and create semaphore */
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + " -- semaphore test..\n");

		if (argv.length == 2) {
			try {
				semkey = Integer.parseInt (argv[1]);
			} catch (NumberFormatException e) {
				semkey = 100 + MPosixIf.getpid ();
			}
		} else {
			semkey = 100 + MPosixIf.getpid ();
		}

		x = MPosixIf.semop (MSemaphore.SEMOP_CREATE, semkey, 0);
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, "semaphore creation failed with " + x + "\n");
			MPosixIf.exit (1);
		}

		/* ignore dying children */
		MPosixIf.signal (MSignal.SIGCHLD, MSignal.SIG_IGN);

		/* start processes that will do wait-style operations */
		for (int i=0; i<3; i++) {
			x = MPosixIf.forkexecc ("/bin/semtest2", new String[] {"/bin/semtest2", "" + semkey, "" + (i+1)});
			if (x < 0) {
				MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": failed to execute /bin/semtest2: " + MStdLib.strerror (x) + "\n");
				MPosixIf.exit (1);
			}
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": started semaphore waiter, pid " + x + "\n");
		}

		/* generate 6 signals, at 1 second intervals */
		for (int i=0; i<6; i++) {
			MPosixIf.sleep (1000);
			x = MPosixIf.semop (MSemaphore.SEMOP_SET, semkey, 1);
			if (x < 0) {
				MPosixIf.writestring (MPosixIf.STDOUT, "semaphore set failed with " + x + "\n");
				MPosixIf.exit (1);
			}
		}

		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": done here, exiting.\n");
		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDOUT, "USemTest signalled with " + signo + "!\n");
	}

}

