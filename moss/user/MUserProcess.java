/*
 *	MUserProcess.java -- anything that wants to be an application/process must implement this
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

package moss.user;


/**
 * This interface describes a user-process within MOSS.  Anything that wants to run with the
 * capabilities of a regular process must implement this.  Kernel-only processes implement
 * the MKernelProcess interface.
 */

public interface MUserProcess
{
	/**
	 * process entry-point
	 *
	 * @param argv "command-line" arguments.  Traditionally,
	 * 		argv[0] is the executable path/name
	 * @param envp process environment
	 *
	 * @return process exit-code.  Traditionally, 0 for success
	 * 		and 1 for failure.
	 */
	public int main (String argv[], MEnv envp);


	/**
	 * method provided to handle signals (when a process asks for them)
	 *
	 * @param signo signal number
	 * @param sigdata signal-specific data (only some signals)
	 */
	public void signal (int signo, Object sigdata);
}


