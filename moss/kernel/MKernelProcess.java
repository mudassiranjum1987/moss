/*
 *	MKernelProcess.java -- anything that wants to be an kernel-thread must implement this
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
 * This interface describes a system-process within MOSS.
 */

public interface MKernelProcess
{
	/**
	 * process entry-point
	 *
	 * @param cmdline "command-line" for the module;  this is the options prefixed by the class-name and executable path
	 */
	public int main (String cmdline[]);

}


