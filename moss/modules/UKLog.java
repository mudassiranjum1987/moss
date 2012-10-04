/*
 *	UKLog.java -- kernel-log message injector utility
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


public class UKLog implements MUserProcess
{
	private String argv[];

	public int main (String argv[], MEnv envp)
	{
		int i;
		String str = "";

		this.argv = argv;
		for (i=1; i<argv.length; i++) {
			str += ((i > 1) ? " " : "") + argv[i];
		}
		MPosixIf.writeklog (str);

		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": signalled with " + signo + "!\n");
		return;
	}
}


