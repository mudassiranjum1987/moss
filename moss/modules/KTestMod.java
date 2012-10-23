/*
 *	KTestMod.java -- test kernel module for MOSS
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

import moss.kernel.*;
import moss.user.*;

public class KTestMod implements MKernelProcess
{
	//{{{  public int main (String argv[])
	/**
	 * entry-point for the kernel module
	 *
	 * @param argv arguments: module name and any options
	 */
	public int main (String argv[])
	{
		MKernel.log_msg ("KTestMod: module loaded and active!  path is [" + argv[1] + "]");
		while (true) {
			MPosixIf.pause ();
		}
	}
	//}}}
}

