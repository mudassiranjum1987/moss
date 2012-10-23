/*
 *	ULoadModule.java -- kernel module loader for MOSS
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

public class ULoadModule implements MUserProcess
{
	public int main (String argv[], MEnv envp)
	{
		String modargs[];
		int modpid;

		if (argv.length < 2) {
			MPosixIf.writestring (MPosixIf.STDERR, "Usage: " + argv[0] + " <module> [args [...]]\n");
			MPosixIf.exit (1);
		}
		modargs = new String[argv.length - 2];
		System.arraycopy (argv, 2, modargs, 0, modargs.length);

		modpid = MPosixIf.loadmodule (argv[1], modargs);
		if (modpid < 0) {
			MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": failed to load module [" + argv[1] + "]: " + MStdLib.strerror (modpid) + "\n");
			MPosixIf.exit (1);
		}

		MPosixIf.writestring (MPosixIf.STDOUT, "module loaded, pid = " + modpid + "\n");

		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDOUT, "ULoadModule signalled with " + signo + "!\n");
	}

}

