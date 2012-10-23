/*
 *	UTimerTest.java -- timer test program
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


public class UTimerTest implements MUserProcess
{
	public int main (String argv[], MEnv envp)
	{
		boolean running = true;

		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": timer test program\n");
		while (running) {
			if (MPosixIf.sleep (1000) < 0) {
				MPosixIf.writestring (MPosixIf.STDOUT, "interrupted!\n");
				running = false;
			}
			MPosixIf.writestring (MPosixIf.STDOUT, "  tick!\n");
		}
		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDOUT, "UTimerTest signalled with " + signo + "!\n");
	}

}

