/*
 *	MDevFS.java -- device file-system
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
import moss.drivers.*;
import moss.user.*;


import java.util.*;

/**
 * this class implements the "device" file-system.  This provides a way of accessing devices
 * through the file-system.
 */

public class MDevFS implements MFSOps
{
	//{{{  private variables/clases/methods/etc.
	
	//{{{  private interface MDevFSEnt
	/**
	 * this class defines an "entity" in the dev file-system
	 */
	private interface MDevFSEnt
	{
		/** gets the name of this entity */
		public String get_name ();
		/** gets the inode of this entity */
		public MInode get_inode ();

		/** returns true if this entity is a directory */
		public boolean isdir ();

	}
	//}}}
	//{{{  private static class MDevFSDir implements MDirOps
	
	private static class MDevFSDir implements MDirOps, MDevFSEnt
	{
		/** name of this directory */
		public String name;
		/** inode for this directory */
		public MInode inode;

		/** contents of this directory */
		public ArrayList contents;
		public Hashtable dirhash;

		//{{{  public MDevFSDir (String name, MInode inode)
		public MDevFSDir (String name, MInode inode)
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
			MDevFSEnt ent;

			if (handle.offset >= dirlen) {
				return -MSystem.ESPIPE;		/* "illegal seek" is probably about the best we can do.. */
			}
			ent = (MDevFSEnt) contents.get (handle.offset);
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

		//{{{  public synchronized void addtodir (MDevFSEnt ent)
		public synchronized void addtodir (MDevFSEnt ent)
		{
			String entname = ent.get_name ();
			Object item = dirhash.get (entname);

			if (item != null) {
				int diridx = contents.lastIndexOf (item);
				MDevFSEnt dent = (MDevFSEnt)item;

				if (diridx < 0) {
					MKernel.panic ("MDevFS: entity in directory hash but not in contents.");
				}
				dent.get_inode().nlinks--;
				dirhash.remove (entname);
				contents.remove (diridx);
			}
			/* add it */
			contents.add ((MDevFSEnt)ent);
			dirhash.put (entname, (MDevFSEnt)ent);
			
			return;
		}
		//}}}
		//{{{  public synchronized void delfromdir (MDevFSEnt ent)
		public synchronized void delfromdir (MDevFSEnt ent)
		{
			String entname = ent.get_name ();
			Object item = dirhash.get (entname);
			int didx;

			if (item == null) {
				/* doesn't exist! */
				return;
			}

			didx = contents.lastIndexOf (item);

			dirhash.remove (entname);
			contents.remove (didx);

			return;
		}
		//}}}
		//{{{  public MDevFSEnt lookup (String name)
		public synchronized MDevFSEnt lookup (String name)
		{
			Object item = dirhash.get (name);

			if (item != null) {
				return (MDevFSEnt)item;
			}
			return null;
		}
		//}}}
	}

	//}}}
	//{{{  private static class MDevFSFile implements MFileOps, MDevFSEnt
	
	private static class MDevFSFile implements MFileOps, MDevFSEnt
	{
		/** name of this file/device */
		public String name;
		/** inode for this file */
		public MInode inode;
		/** actual file-operations for device */
		public MFileOps devops;


		//{{{  public MDevFSFile (String name, MInode inode)
		public MDevFSFile (String name, MInode inode)
		{
			this.name = name;
			this.inode = inode;
			this.devops = null;
		}
		//}}}

		//{{{  public synchronized int open (MFile handle, int flags)
		public synchronized int open (MFile handle, int flags)
		{
			int r;

			handle.pdata = null;	/* device may use this */
			handle.fileif = this;
			handle.offset = 0;
			handle.inode = inode;

			r = MDevices.open (handle, inode.major, inode.minor, flags);
			if (r < 0) {
				handle.pdata = null;
				handle.fileif = null;
				handle.inode = null;
				return r;
			}

			/* been fiddled by device */
			devops = handle.fileif;
			handle.fileif = this;

			return 0;
		}
		//}}}
		//{{{  public synchronized int close (MFile handle)
		public synchronized int close (MFile handle)
		{
			int r;

			r = devops.close (handle);

			handle.pdata = null;
			handle.fileif = null;
			handle.inode = null;

			return r;
		}
		//}}}
		//{{{  public synchronized int lseek (MFile handle, int offset, int whence)
		public synchronized int lseek (MFile handle, int offset, int whence)
		{
			int r;

			r = devops.lseek (handle, offset, whence);
			return r;
		}
		//}}}
		//{{{  public synchronized int read (MFile handle, byte buffer[], int count)
		public synchronized int read (MFile handle, byte buffer[], int count)
		{
			int r;

			r = devops.read (handle, buffer, count);
			return r;
		}
		//}}}
		//{{{  public synchronized int write (MFile handle, byte buffer[], int count)
		public synchronized int write (MFile handle, byte buffer[], int count)
		{
			int r;

			r = devops.write (handle, buffer, count);
			return r;
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

	//{{{  private static class PFS_devfs implements MProcFSIf
	/**
	 * this class deals with the contents of /proc/devfs
	 */
	private static class PFS_devfs implements MProcFSIf
	{
		public String readproc (MInode inode, String name)
		{
			String r;

			r = "MDevFS file-system\n";

			return r;
		}
	}
	//}}}

	/** general lock for static data */
	private static Object synclock = new Object ();
	/** root of the file-system (only ever 1 of these) */
	private static MDevFSEnt root = null;
	/** ever-incrementing inode counter (!) */
	private static int icount = 0;
	/** mount count */
	private static int mount_count = 0;

	//{{{  private static MDevFSEnt newfsent (String name, int mode, int uid, int gid, int major, int minor)
	private static MDevFSEnt newfsent (String name, int mode, int uid, int gid, int major, int minor)
	{
		MDevFSEnt ent;
		MInode ino = new MInode ();

		if ((mode & MInode.S_IFMT) == MInode.S_IFDIR) {
			ent = new MDevFSDir (new String (name), ino);
		} else {
			ent = new MDevFSFile (new String (name), ino);
		}
		ino.ino = icount++;
		ino.mode = mode;
		ino.nlinks = 1;
		ino.uid = uid;
		ino.gid = gid;
		ino.major = major;
		ino.minor = minor;

		return ent;
	}
	//}}}
	//{{{  private static MDevFSEnt newfsent (String name, MDevFSEnt entity)
	private static MDevFSEnt newfsent (String name, MDevFSEnt entity)
	{
		MDevFSEnt ent;
		MInode ino = entity.get_inode ();

		if ((ino.mode & MInode.S_IFMT) == MInode.S_IFDIR) {
			/* duplicating directory link */
			ent = new MDevFSDir (new String (name), ino);
		} else {
			/* duplicating ordinary entry */
			ent = new MDevFSFile (new String (name), ino);
		}
		ino.nlinks++;

		return ent;
	}
	//}}}

	//{{{  private static String[] split_path (String path)
	private static String[] split_path (String path)
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
	//{{{  private static MDevFSEnt lookup (String bits[])
	/**
	 * starting at the root, this scans the file-system for something
	 */
	private static MDevFSEnt lookup (String bits[])
	{
		MDevFSEnt ent = root;

		for (int idx = 0; idx < bits.length; idx++) {
			MDevFSEnt newent;

			if (!ent.isdir ()) {
				return null;
			}
			newent = ((MDevFSDir)ent).lookup (bits[idx]);
			if (newent == null) {
				return null;
			}
			ent = newent;
		}
		return ent;
	}
	//}}}


	//{{{  private static void init_dev_root ()
	/**
	 * initialises the device root file-system
	 */
	private static void init_dev_root ()
	{
		MDevFSEnt ent;
		MDevFSDir rootdir;

		synchronized (synclock) {
			if (root != null) {
				return;
			}

			icount = 1;
			root = newfsent ("", (0755 | MInode.S_IFDIR), 0, 0, -1, -1);
		}
		rootdir = (MDevFSDir)root;

		ent = newfsent (".", root);
		rootdir.addtodir (ent);
		ent = newfsent ("..", root);
		rootdir.addtodir (ent);

		return;
	}
	//}}}

	//}}}

	//{{{  public static void finalinit ()
	/**
	 * called to perform final kernel initialisation (in the context of the init-task)
	 */
	public static void finalinit ()
	{
		MProcFS.register_procimpl ("devfs", new PFS_devfs());
	}
	//}}}

	//{{{  public static MInode register_device (String name, int major, int minor, int mode)
	/**
	 * registers a device with the device file-system
	 *
	 * @param name entry-name.  may be something like "foo/bar", but the "foo" must exist first
	 * @param major major device number
	 * @param minor minor device number
	 * @param mode mode of the device
	 *
	 * @return an MInode for the entry, or null on error (if it already exists)
	 */
	public static MInode register_device (String name, int major, int minor, int mode)
	{
		MDevFSEnt parent, ent;
		String bits[] = split_path (name);

		init_dev_root ();		/* if not already */

		ent = lookup (bits);
		if (ent != null) {
			/* already exists */
			return null;
		}

		/* creating it */
		if (bits.length == 1) {
			parent = root;
		} else {
			String newbits[] = new String[bits.length - 1];

			System.arraycopy (bits, 0, newbits, 0, newbits.length);
			parent = lookup (newbits);

			if (parent == null) {
				return null;
			}
		}
		ent = newfsent (bits[bits.length - 1], mode, 0, 0, major, minor);

		/* stick in parent */
		((MDevFSDir)parent).addtodir (ent);

		return ent.get_inode();
	}
	//}}}
	//{{{  public static int unregister_device (String name)
	/**
	 * unregisters something from the device file-system
	 *
	 * @param name entry-name.  may be something like "foo/bar"
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int unregister_device (String name)
	{
		MDevFSEnt ent, parent;
		String bits[] = split_path (name);

		init_dev_root ();		/* make sure.. */

		ent = lookup (bits);
		if (ent == null) {
			return -MSystem.ENOENT;
		}

		if (bits.length == 1) {
			parent = root;
		} else {
			String newbits[] = new String[bits.length - 1];

			System.arraycopy (bits, 0, newbits, 0, newbits.length);
			parent = lookup (newbits);

			if (parent == null) {
				return -MSystem.ENOENT;
			}
		}

		/* remove it */
		((MDevFSDir)parent).delfromdir (ent);
		
		return 0;
	}
	//}}}
	//{{{  public static int register_devdir (String name)
	/**
	 * creates a directory in the device file-system
	 *
	 * @param name name of the directory to create
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int register_devdir (String name)
	{
		MDevFSEnt ent, parent;
		String bits[] = split_path (name);

		init_dev_root ();		/* make sure.. */

		ent = lookup (bits);
		if (ent != null) {
			return -MSystem.EEXISTS;
		}

		if (bits.length == 1) {
			parent = root;
		} else {
			String newbits[] = new String[bits.length - 1];

			System.arraycopy (bits, 0, newbits, 0, newbits.length);
			parent = lookup (newbits);

			if (parent == null) {
				return -MSystem.ENOENT;
			}
		}
		ent = newfsent (bits[bits.length - 1], (0755 | MInode.S_IFDIR), 0, 0, -1, -1);

		/* add it to the parent */
		((MDevFSDir)parent).addtodir (ent);

		/* add the two special directory entries to it */
		((MDevFSDir)ent).addtodir (newfsent (".", ent));
		((MDevFSDir)ent).addtodir (newfsent ("..", parent));

		return 0;
	}
	//}}}
	//{{{  public static int unregister_devdir (String name)
	/**
	 * removes a directory in the device file-system
	 *
	 * @param name name of the directory to remove
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int unregister_devdir (String name)
	{
		MDevFSEnt ent, parent;
		MDevFSEnt dent, ddent;
		String bits[] = split_path (name);

		init_dev_root ();		/* make sure.. */

		ent = lookup (bits);
		if (ent == null) {
			return -MSystem.ENOENT;
		} else if (!ent.isdir()) {
			return -MSystem.ENOTDIR;
		}

		if (bits.length == 1) {
			parent = root;
		} else {
			String newbits[] = new String[bits.length - 1];

			System.arraycopy (bits, 0, newbits, 0, newbits.length);
			parent = lookup (newbits);

			if (parent == null) {
				return -MSystem.ENOENT;
			}
		}

		/* remove the two special entries */
		{
			String newbits[] = new String[bits.length + 1];
			
			System.arraycopy (bits, 0, newbits, 0, bits.length);
			newbits[bits.length] = ".";
			dent = lookup (newbits);
			newbits[bits.length] = "..";
			ddent = lookup (newbits);
		}

		if ((dent == null) || (ddent == null)) {
			return -MSystem.EIO;
		}

		((MDevFSDir)ent).delfromdir (dent);
		((MDevFSDir)ent).delfromdir (ddent);

		/* remove it */
		((MDevFSDir)parent).delfromdir (ent);
		
		return 0;
	}
	//}}}


	//{{{  public int mount (String mount_path, String options[])
	/**
	 * called when the device file-system is mounted.  Performs specific initialisation.
	 *
         * @param mount_path full path from the root (/) to where this is being mounted.  A single
	 * 			"/" indicates that this is being mounted as the root file-system.
	 * @param options file-system specific options
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int mount (String mount_path, String options[])
	{

		// MKernel.log_msg ("MDevFS: mounting on [" + mount_path + "]");

		init_dev_root ();
		synchronized (synclock) {
			mount_count++;
		}
		return 0;
	}
	//}}}
	//{{{  public int umount (String options[])
	/**
	 * this is called before the device file-system is un-mounted.
	 *
	 * @param options file-system specific un-mounting options
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int umount (String options[])
	{
		synchronized (synclock) {
			mount_count--;
		}
		return 0;
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
		MDevFSEnt ent = null;
		MDevFSFile fileent = null;
		String bpath[];

		if (path.equals ("/")) {
			ent = root;
			bpath = new String[] {path};
		} else {
			bpath = split_path (path);
			ent = lookup (bpath);
		}

		if (ent == null) {
			return -MSystem.ENOENT;
		} else if (ent.isdir()) {
			return -MSystem.EISDIR;
		} /* else ent is a valid file */

		fileent = (MDevFSFile)ent;

		if ((flags & MFileOps.OPEN_WRITE) != 0) {
			/* not allowed to write dev-fs stuff (yet..) */
			return -MSystem.EPERM;
		}

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
		MDevFSEnt ent = null;
		MDevFSDir dirent = null;

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
		dirent = (MDevFSDir)ent;

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
		/* not the device file-system.. */
		return -MSystem.EIO;
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
		return -MSystem.EIO;
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
		return -MSystem.EIO;
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
		return -MSystem.EIO;
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
		return -MSystem.EIO;
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
		return -MSystem.EIO;
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
		return -MSystem.EIO;
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
		return -MSystem.EIO;
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
		MDevFSEnt ent;
		String bits[];

		// MKernel.log_msg ("MDevFS::stat.  path = [" + path + "]");
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
		MDevFSEnt ent;
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


