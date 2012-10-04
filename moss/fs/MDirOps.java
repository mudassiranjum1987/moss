/*
 *	MDirOps.java -- interface for things that we can treat like a directory
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
 * This interface defines kernel "directory-operations".  Its use is somewhat
 * limited to actual directory handling (unlike MFileOps).
 */

public interface MDirOps
{
	/**
	 * used to open a directory.  This is only ever called once for each file (directory) handle
	 *
	 * @param handle file-handle of directory being opened
	 *
	 * @return 0 on success or &lt; 0 indicating error
	 */
	public int open (MFile handle);

	/**
	 * used to close a directory.  This is only ever called once for each file (directory) handle
	 *
	 * @param handle file-handle of the directory being closed
	 *
	 * @return 0 on success or &lt; 0 indicating error
	 */
	public int close (MFile handle);

	/**
	 * used to read a directory entry.
	 *
	 * @param handle file-handle of the directory to read from
	 * @param dirent MDirEnt in which the directory entry is stored
	 *
	 * @return 0 on success or &lt; 0 indicating error (including end-of-directory)
	 */
	public int readdir (MFile handle, MDirEnt dirent);

}


