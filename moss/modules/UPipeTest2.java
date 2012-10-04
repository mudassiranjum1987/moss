/*
 *	UPipeTest2.java -- pipe-testing program (reading half)
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


public class UPipeTest2 implements MUserProcess
{
	public int main (String argv[], MEnv envp)
	{
		int fd = -1;
		int x;
		byte buffer[];
		
		/* say hello and get pipe descriptor */
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + " -- pipe test (reading half)..\n");

		if (argv.length < 2) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": um, incorrect usage!\n");
			MPosixIf.exit (1);
		}
		try {
			fd = Integer.parseInt (argv[1]);
		} catch (NumberFormatException e) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": bad argument [" + argv[1] + "]\n");
			MPosixIf.exit (1);
		}

		/* read something */
		buffer = new byte[256];
		x = MPosixIf.read (fd, buffer, buffer.length);
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": read error " + x + "\n");
			MPosixIf.exit (1);
		} else {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": got this [");
			MPosixIf.write (MPosixIf.STDOUT, buffer, x);
			MPosixIf.writestring (MPosixIf.STDOUT, "]\n");
		}
		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDOUT, "UPipeTest2 signalled with " + signo + "!\n");
	}

}

