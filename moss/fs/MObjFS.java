/*
 *	MObjFS.java -- `object' file-system
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

import moss.kernel.*;
import moss.user.*;

import java.util.*;

/**
 * this class implements the "object" file-system.  This is a free-hanging object-oriented-ish
 * file-system.
 */

public class MObjFS implements MFSOps
{
	//{{{  private variables/clases/methods/etc.
	
	//{{{  private interface MObjFSEnt
	/**
	 * this class defines an "entity" in the object file-system
	 */
	private interface MObjFSEnt
	{
		/** gets the name of this entity */
		public String get_name ();
		/** gets the inode of this entity */
		public MInode get_inode ();

		/** returns true if this entity is a directory */
		public boolean isdir ();

	}
	//}}}
	//{{{  private class MObjFSDir implements MDirOps, MObjFSEnt
	
	private class MObjFSDir implements MDirOps, MObjFSEnt
	{
		/** name of this directory */
		public String name;
		/** inode for this directory */
		public MInode inode;

		/** contents of this directory */
		public ArrayList contents;
		public Hashtable dirhash;

		//{{{  public MObjFSDir (String name, MInode inode)
		public MObjFSDir (String name, MInode inode)
		{
			this.name = name;
			this.inode = inode;
			this.contents = new ArrayList ();
			this.dirhash = new Hashtable ();
		}
		//}}}

		//{{{  public synchronized int open (MFile handle)
		public synchronized int open (MFile handle)
		{
			handle.pdata = (Object)this;
			handle.dirif = this;
			handle.offset = 0;

			return 0;
		}
		//}}}
		//{{{  public synchronized int close (MFile handle)
		public synchronized int close (MFile handle)
		{
			handle.pdata = null;

			return 0;
		}
		//}}}
		//{{{  public synchronized int readdir (MFile handle, MDirEnt dirent)
		public synchronized int readdir (MFile handle, MDirEnt dirent)
		{
			int dirlen = contents.size ();
			MObjFSEnt ent;

			if (handle.offset >= dirlen) {
				return -MSystem.ESPIPE;		/* "illegal seek" is probably about the best we can do.. */
			}
			ent = (MObjFSEnt) contents.get (handle.offset);
			dirent.d_name = new String(ent.get_name ());
			dirent.d_ino = (ent.get_inode ()).ino;
			dirent.d_mode = (ent.get_inode ()).mode;
			handle.offset++;

			return 0;
		}
		//}}}

		//{{{  public String get_name ()
		public String get_name ()
		{
			return name;
		}
		//}}}
		//{{{  public MInode get_inode ()
		public MInode get_inode ()
		{
			return inode;
		}
		//}}}
		//{{{  public boolean isdir ()
		/**
		 * returns true.
		 */
		public boolean isdir ()
		{
			return true;
		}
		//}}}

		//{{{  public void addtodir (MObjFSEnt ent)
		public synchronized void addtodir (MObjFSEnt ent)
		{
			String entname = ent.get_name ();
			Object item = dirhash.get (entname);

			if (item != null) {
				int diridx = contents.lastIndexOf (item);
				MObjFSEnt dent = (MObjFSEnt)item;

				if (diridx < 0) {
					MKernel.panic ("MObjFS: entity in directory hash but not in contents.");
				}
				dent.get_inode().nlinks--;
				dirhash.remove (entname);
				contents.remove (diridx);
			}
			/* add it */
			contents.add ((MObjFSEnt)ent);
			dirhash.put (entname, (MObjFSEnt)ent);
		}
		//}}}
		//{{{  public MObjFSEnt lookup (String name)
		public synchronized MObjFSEnt lookup (String name)
		{
			Object item = dirhash.get (name);

			if (item != null) {
				return (MObjFSEnt)item;
			}
			return null;
		}
		//}}}
	}

	//}}}
	//{{{  private class MObjFSFile implements MFileOps, MObjFSEnt
	
	private class MObjFSFile implements MFileOps, MObjFSEnt
	{
		/** name of this file */
		public String name;
		/** inode for this file */
		public MInode inode;

		/** contents of this file */
		public byte data[];

		//{{{  public MObjFSFile (String name, MInode inode)
		public MObjFSFile (String name, MInode inode)
		{
			this.name = name;
			this.inode = inode;
			this.data = null;
		}
		//}}}

		//{{{  public synchronized int open (MFile handle, int flags)
		public synchronized int open (MFile handle, int flags)
		{
			handle.pdata = (Object)this;
			handle.fileif = this;
			handle.offset = 0;
			if ((flags & MFileOps.OPEN_TRUNC) != 0) {
				/* truncate contents of the file if already set */
				this.data = null;
				this.inode.size = 0;
			}
			return 0;
		}
		//}}}
		//{{{  public synchronized int close (MFile handle)
		public synchronized int close (MFile handle)
		{
			handle.pdata = null;

			return 0;
		}
		//}}}
		//{{{  public synchronized int lseek (MFile handle, int offset, int whence)
		public synchronized int lseek (MFile handle, int offset, int whence)
		{
			/* make offset absolute */
			switch (whence) {
			case LSEEK_BEG:
				break;
			case LSEEK_CUR:
				offset = handle.offset + offset;
				break;
			case LSEEK_END:
				offset = (int)inode.size - offset;
				break;
			}
			if (offset < 0) {
				offset = 0;
			} else if (offset > inode.size) {
				offset = (int)inode.size;
			}

			handle.offset = offset;

			return handle.offset;
		}
		//}}}
		//{{{  public synchronized int read (MFile handle, byte buffer[], int count)
		public synchronized int read (MFile handle, byte buffer[], int count)
		{
			if (handle.offset >= inode.size) {
				/* end-of-file */
				return 0;
			}
			if (count > (inode.size - handle.offset)) {
				count = ((int)inode.size - handle.offset);
			}

			/* read some data */
			System.arraycopy (data, handle.offset, buffer, 0, count);
			handle.offset += count;

			return count;
		}
		//}}}
		//{{{  public synchronized int write (MFile handle, byte buffer[], int count)
		public synchronized int write (MFile handle, byte buffer[], int count)
		{
			if (data == null) {
				/* need to create! */
				data = new byte[handle.offset + count];
			}
			if ((handle.offset + count) > data.length) {
				/* writing past end-of-file, enlarge */
				byte newdata[] = new byte[(handle.offset + count)];

				System.arraycopy (data, 0, newdata, 0, data.length);
				data = newdata;
			}

			/* write some data */
			System.arraycopy (buffer, 0, data, handle.offset, count);
			handle.offset += count;
			if (handle.offset > inode.size) {
				inode.size = handle.offset;		/* got bigger */
			}

			return count;
		}
		//}}}
		//{{{  public synchronized int fcntl (MFile handle, int op, int arg)
		public synchronized int fcntl (MFile handle, int op, int arg)
		{
			/* not yet.. */
			return -MSystem.EIO;
		}
		//}}}

		//{{{  public boolean isdir ()
		/**
		 * returns false.
		 */
		public boolean isdir ()
		{
			return false;
		}
		//}}}
		//{{{  public synchronized String get_name ()
		public synchronized String get_name ()
		{
			return name;
		}
		//}}}
		//{{{  public synchronized MInode get_inode ()
		public synchronized MInode get_inode ()
		{
			return inode;
		}
		//}}}
	}

	//}}}

	/** root of the file-system */
	private MObjFSEnt root;
	/** ever-incrementing inode counter (!) */
	private int icount;

	//{{{  private MObjFSEnt newfsent (String name, int mode, int uid, int gid)
	private MObjFSEnt newfsent (String name, int mode, int uid, int gid)
	{
		MObjFSEnt ent;
		MInode ino = new MInode ();

		if ((mode & MInode.S_IFMT) == MInode.S_IFDIR) {
			ent = new MObjFSDir (new String (name), ino);
		} else {
			ent = new MObjFSFile (new String (name), ino);
		}
		ino.ino = icount++;
		ino.mode = mode;
		ino.nlinks = 1;
		ino.uid = uid;
		ino.gid = gid;

		return ent;
	}
	//}}}
	//{{{  private MObjFSEnt newfsent (String name, MObjFSEnt entity)
	private MObjFSEnt newfsent (String name, MObjFSEnt entity)
	{
		MObjFSEnt ent;
		MInode ino = entity.get_inode ();

		if ((ino.mode & MInode.S_IFMT) == MInode.S_IFDIR) {
			/* duplicating directory link */
			ent = new MObjFSDir (new String (name), ino);
		} else {
			/* duplicating ordinary entry */
			ent = new MObjFSFile (new String (name), ino);
		}
		ino.nlinks++;

		return ent;
	}
	//}}}

	//{{{  private String[] split_path (String path)
	private String[] split_path (String path)
	{
		int nbits, j;
		String bits[];
		int pl;

		pl = path.length ();
		if (path.charAt (pl-1) == '/') {
			/* if it ends in a /, lose it */
			pl--;
		}

		nbits = 1;
		for (int i=0; i<pl; i++) {
			if (path.charAt(i) == '/') {
				nbits++;
			}
		}
		bits = new String[nbits];

		j = 0;
		nbits = 0;
		for (int i=0; i<pl; i++) {
			if (path.charAt(i) == '/') {
				bits[nbits] = path.substring (j, i);
				j = i+1;
				nbits++;
			}
		}
		if (j < pl) {
			bits[nbits] = path.substring (j, pl);
		}

		return bits;
	}
	//}}}
	//{{{  private MObjFSEnt lookup (String bits[])
	/**
	 * starting at the root, this scans the file-system for something
	 */
	private MObjFSEnt lookup (String bits[])
	{
		MObjFSEnt ent = root;

		for (int idx = 0; idx < bits.length; idx++) {
			MObjFSEnt newent;

			if (!ent.isdir ()) {
				return null;
			}
			newent = ((MObjFSDir)ent).lookup (bits[idx]);
			if (newent == null) {
				return null;
			}
			ent = newent;
		}
		return ent;
	}
	//}}}
	//}}}


	//{{{  public int mount (String mount_path, String options[])
	/**
	 * called when the object file-system is mounted.  Performs specific initialisation.
	 *
         * @param mount_path full path from the root (/) to where this is being mounted.  A single
	 * 			"/" indicates that this is being mounted as the root file-system.
	 * @param options file-system specific options
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int mount (String mount_path, String options[])
	{
		MObjFSEnt ent;
		MObjFSDir rootdir;

		// MKernel.log_msg ("MObjFS: mounting on [" + mount_path + "]");

		icount = 1;
		root = newfsent ("", (0755 | MInode.S_IFDIR), 0, 0);
		rootdir = (MObjFSDir)root;

		ent = newfsent (".", root);
		rootdir.addtodir (ent);
		ent = newfsent ("..", root);
		rootdir.addtodir (ent);

		return 0;
	}
	//}}}
	//{{{  public int umount (String options[])
	/**
	 * this is called before the object file-system is un-mounted.
	 *
	 * @param options file-system specific un-mounting options
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int umount (String options[])
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int open (String path, MFile handle, int flags)
	/**
	 * called to open a file.
	 *
	 * @param path relative path to the file (e.g. `etc/passwd' for the root file-system)
	 * @param handle file-handle to be associated with this file
	 * @param flags open flags
	 * @param mode file mode if creating
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int open (String path, MFile handle, int flags, int mode)
	{
		MObjFSEnt ent = null;
		MObjFSFile fileent = null;
		String bpath[];

		if (path.equals ("/")) {
			ent = root;
			bpath = new String[] {path};
		} else {
			bpath = split_path (path);
			ent = lookup (bpath);
		}
		if ((ent == null) && ((flags & MFileOps.OPEN_CREAT) != 0)) {
			/* creating the file */
			MObjFSEnt parent;

			if (bpath.length == 1) {
				parent = root;
			} else {
				String newbits[] = new String[bpath.length - 1];

				System.arraycopy (bpath, 0, newbits, 0, bpath.length - 1);
				parent = lookup (newbits);
			}
			if (parent == null) {
				return -MSystem.ENOENT;
			} else if (!parent.isdir ()) {
				return -MSystem.ENOTDIR;
			}
			ent = newfsent (bpath[bpath.length - 1], MInode.S_IFREG | (mode & MInode.S_IMPERM), 0, 0);

			/* stick in parent */
			((MObjFSDir)parent).addtodir (ent);
		} else if (ent == null) {
			return -MSystem.ENOENT;
		} else if (ent.isdir()) {
			return -MSystem.EISDIR;
		} /* else ent is a valid file */

		fileent = (MObjFSFile)ent;

		/* open it */
		return fileent.open (handle, flags);
	}
	//}}}
	//{{{  public int opendir (String path, MFile handle)
	/**
	 * called to open a directory (read-only!).
	 *
	 * @param path relative path to the directory (e.g. `etc/init.d' for the root file-system)
	 * @param handle file-handle to be associated with this directory
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int opendir (String path, MFile handle)
	{
		MObjFSEnt ent = null;
		MObjFSDir dirent = null;

		if (path.equals ("/")) {
			ent = root;
		} else {
			String bpath[] = split_path (path);

			ent = lookup (bpath);
		}
		if (ent == null) {
			return -MSystem.ENOENT;
		} else if (!ent.isdir()) {
			return -MSystem.ENOTDIR;
		}
		/* permission checks, etc. here */
		dirent = (MObjFSDir)ent;

		/* open it */
		return dirent.open (handle);
	}
	//}}}
	//{{{  public int mkdir (String path)
	/**
	 * creates a directory in the file-system
	 *
	 * @param path relative path to the directory to be created
	 * @param flags flags (mode)
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int mkdir (String path, int flags)
	{
		MObjFSEnt newdir, parent, ent;
		String bits[];

		if (path.equals ("/")) {
			return -MSystem.EEXISTS;
		}
		bits = split_path (path);

		/* see if the new thing already exists */
		newdir = lookup (bits);

		if (newdir != null) {
			return -MSystem.EEXISTS;
		}

		/* last component of "bits" is the directory we want to create */
		if (bits.length == 1) {
			parent = root;
		} else {
			String newbits[] = new String[bits.length - 1];

			System.arraycopy (bits, 0, newbits, 0, bits.length - 1);
			parent = lookup (newbits);
		}

		if (parent == null) {
			return -MSystem.ENOENT;
		} else if (!parent.isdir ()) {
			return -MSystem.ENOTDIR;
		}

		newdir = newfsent (bits[bits.length - 1], ((flags & MInode.S_IMPERM) | MInode.S_IFDIR), 0, 0);
		/* need to put two nodes in the directory, self and parent */

		ent = newfsent (".", newdir);
		((MObjFSDir)newdir).addtodir (ent);
		ent = newfsent ("..", parent);
		((MObjFSDir)newdir).addtodir (ent);

		((MObjFSDir)parent).addtodir (newdir);

		return 0;
	}
	//}}}
	//{{{  public int unlink (String path)
	/**
	 * removes a name from the file-system
	 *
	 * @param path relative path to the name to be removed
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int unlink (String path)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int rmdir (String path)
	/**
	 * removes a directory from the file-system (must be empty)
	 *
	 * @param path relative path to the directory to be removed
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int rmdir (String path)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int link (String oldpath, String newpath)
	/**
	 * creates a `hard-link'
	 *
	 * @param oldpath existing relative path (e.g. local/packages/nmh for "usr" file-system)
	 * @param newpath new relative path
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int link (String oldpath, String newpath)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int symlink (String oldpath, String newpath)
	/**
	 * creates a symbolic link
	 *
	 * @param oldpath full path to existing name (e.g. /usr/local/packages/nmh-1.4 for "usr" file-system)
	 * @param newpath relative path to new link (e.g. local/packages/nmh for "usr" file-system)
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int symlink (String oldpath, String newpath)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int mknod (String path, int mode, int majmin)
	/**
	 * creates a special file on the file-system
	 *
	 * @param path relative path to file to create
	 * @param mode file mode flags
	 * @param majmin major and minor numbers `<tt>((major &lt;&lt; 16) | minor)</tt>' for block/char special files,
	 * 			otherwise ignored.
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int mknod (String path, int mode, int majmin)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int readlink (String path, byte[] buf, int buflen)
	/**
	 * reads the contents of a symbolic-link on the file-system
	 *
	 * @param path relative path to symbolic link on file-system
	 * @param buf buffer where the link data is stored
	 * @param buflen maximum number of bytes to write into `buf'
	 *
	 * @return number of bytes read on success, or &lt; 0 indicating error
	 */
	public int readlink (String path, byte[] buf, int buflen)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int utime (String path, long[] times)
	/**
	 * sets access and modification times for a file (inode)
	 *
	 * @param path relative path to name
	 * @param times array of two `<tt>long</tt>'s that are the access and modification times respectively
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int utime (String path, long[] times)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int stat (String path, MInode statbuf)
	/**
	 * retrieves information about a name on the file-system
	 *
	 * @param path relative path to name
	 * @param statbuf buffer in which information is placed
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int stat (String path, MInode statbuf)
	{
		MObjFSEnt ent;
		String bits[];

		if (path.equals ("/")) {
			ent = root;
		} else {
			bits = split_path (path);
			ent = lookup (bits);
		}

		if (ent == null) {
			return -MSystem.ENOENT;
		}

		/* read out inode data */
		synchronized (ent) {
			MInode ent_inode = ent.get_inode();

			statbuf.major = ent_inode.major;
			statbuf.minor = ent_inode.minor;
			statbuf.ino = ent_inode.ino;
			statbuf.mode = ent_inode.mode;
			statbuf.nlinks = ent_inode.nlinks;
			statbuf.uid = ent_inode.uid;
			statbuf.gid = ent_inode.gid;
			statbuf.size = ent_inode.size;
			statbuf.blksize = ent_inode.blksize;
			statbuf.nblocks = ent_inode.nblocks;
			statbuf.atime = ent_inode.atime;
			statbuf.mtime = ent_inode.mtime;
			statbuf.ctime = ent_inode.ctime;
		}
		return 0;
	}
	//}}}
	//{{{  public int access (String path, int amode)
	/**
	 * tests for access to a file
	 *
	 * @param path relative path to file/directory
	 * @param amode access mode
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int access (String path, int amode)
	{
		MObjFSEnt ent;
		String bits[];
		int r = 0;
		int imode;

		if (path.equals ("/")) {
			ent = root;
		} else {
			bits = split_path (path);
			ent = lookup (bits);
		}

		if (ent == null) {
			return -MSystem.ENOENT;
		}
		synchronized (ent) {
			MInode ino = ent.get_inode ();

			imode = ino.mode;
		}
		
		/* these checks are slightly blind.. (check for any read/write/execute) */
		if (((amode & MFileOps.R_OK) != 0) && ((imode & 0444) == 0)) {
			return -MSystem.EPERM;
		}
		if (((amode & MFileOps.W_OK) != 0) && ((imode & 0222) == 0)) {
			return -MSystem.EPERM;
		}
		if (((amode & MFileOps.X_OK) != 0) && ((imode & 0111) == 0)) {
			return -MSystem.EPERM;
		}

		return 0;
	}
	//}}}
}


