/*
 *	MProcFSIf.java -- interface for things wishing to provide information to MProcFS
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


package moss.fs;

/**
 * This interface allows various parts of the (MOSS) kernel to provide information
 * to other processes via the "process" file-system (generally mounted on /proc).
 */

public interface MProcFSIf
{
	/**
	 * called to read data
	 *
	 * @param inode MProcFS inode associated with the file
	 * @param name filename in the proc filesystem
	 *
	 * @return String containing the data
	 */
	public String readproc (MInode inode, String name);

}


