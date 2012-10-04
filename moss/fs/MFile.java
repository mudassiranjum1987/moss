/*
 *	MFile.java -- "File" class, used for referencing open file objects
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
 * This class is used to represent a "file-handle".  It contains some basic
 * data, plus a reference to the interface that performs operations for it.
 */
public class MFile
{
	/** reference count */
	public int refcount = 0;
	/** private data hook for implementations */
	public Object pdata = null;
	/** current file offset */
	public int offset = 0;
	/** flags associated with this descriptor (MFileOps.O_...) */
	public int flags = 0;

	/** implementation (for files) */
	public MFileOps fileif;
	/** implementation (for directories) */
	public MDirOps dirif;

	/** inode (if related to a file-system object) */
	public MInode inode;
}


