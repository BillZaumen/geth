package org.bzdev.swing;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import java.util.Stack;

//@exbundle org.bzdev.swing.lpack.Swing

/**
 * ObjTocPane provides a JTree that provides a table of contents for
 * a set of objects.  The table of contents is arranged hierarchically
 * with a single root.  The tree is configured to support single
 * selections.  Each time a node is selected (including being selected
 * a second time without first deselecting it), all action listeners
 * are notified.
 * <P>
 * To initialize the tree, the user should first call its
 * constructor (which takes zero arguments), followed by a call to the
 * method <code>addEntry(name, obj)</code> to add the root of the
 * tree. This assigns the root node a name <code>name</code> and value
 * <code>obj</code>.  To add children, this must be followed by a call
 * to <code>nextLevel</code>, followed by calls to
 * <code>addEntry</code> for each child.  Each call to
 * <code>nextLevel()</code> must be balanced with a call to
 * <code>prevLevel()</code>.  When all entries are defined, the method
 * <code>entriesCompleted()</code>.  If you do not want the root node
 * to be displayed, use <code>entriesCompleted(false)</code> instead
 * of <code>entriesCompleted()</code>.  One may also use the
 * <code>addActionListener</code> method to add action listeners in order
 * to respond to changes in the selection.  These listeners are notified
 * whenever an entry is clicked, not just when the entry was changed. 
 *<P>
 * Finally, one should call {@link ObjTocPane#setSelectionWithAction(int)} to
 * execute the action that would occur when the initial node is
 * selected (this is not done automatically to give the programmer
 * some control over when this should occur, and allows software to
 * change the selection as needed.)
 *<P>
 * Access methods for {@link ObjTocPane.Entry} allow one to recover
 * any relevant mouse event. These are valid for the duration of
 * action listeners fired by a selection or a call to
 * <code>setSelectionWithAction(int)</code>.  An instance of
 * {@link ObjTocPane.Entry} will be used as the source of action events
 * created by this class in response to a call to
 * {@link ObjTocPane#setSelectionWithAction(int)}.
 * @author Bill Zaumen
 * @version $Revision: 1.5 $, $Date: 2005/04/27 06:48:23 $
 */

public class ObjTocPane extends JTree implements TocTree {

    static String errorMsg(String key, Object... args) {
	return SwingErrorMsg.errorMsg(key, args);
    }

    /**
     * An entry for a table of contents.
     * Instances of this class are passed to action listeners as the
     * source of an <code>ActionEvent</code>.  A default entry exists
     * for a missing selection.
     */
    public class Entry {
	JTree parent;
	String title;
	Object obj;
	Entry(JTree parent, String name, Object obj) {
	    this.parent = parent;
	    this.title = name;
	    this.obj = obj;
	}
	public String toString() {return title;}

	/**
	 * get the title for this entry.
	 * The title is the name that will appear in the component
	 * @return the title; null if none.
	 */
	public String getTitle() {return title;}

	/**
	 * Get the value for this entry.
	 * The value is the object associated with the selected row.
	 * @return the value; null if none.
	 */
	public Object getValue() {return obj;}

	private MouseEvent mouseEvent = null;

	/**
	 * Get the mouseEvent that triggered an action listener.
	 * Calls to this method are valid only in action listeners.
	 * @return the last mouse event for this object; null if there is none.
	 */
	public MouseEvent getMouseEvent() {return mouseEvent;}

	/**
	 * Get the parent of this Entry.
	 * THe parent will be the \texttt{ObjTocPane} that generated
	 * this event.
	 * Due to the possible generation of multiple events, some of
	 * the values returned by the parent's access methods may change
	 * immediately after an action event is processed. One should
	 * only trust access methods that such a sequence of events will
	 * not change. One can defer execution of some code by using
	 * methods from {@link javax.swing.SwingUtilities}.
	 * @return the JTree that contains this Entry.
	 */
	public JTree getParent() {return parent;}
    }

    private DefaultMutableTreeNode toc = null;

    private DefaultMutableTreeNode current = null;

    private Entry deselected = new Entry(this, null, null);
    private Entry  selected = null;

    private Stack<DefaultMutableTreeNode> nstack = 
	new Stack<DefaultMutableTreeNode>();

    /**
     * Clear the table of contents and puts it in a state where entries
     * can be added.
     */
    public void clearToc() {
	clearSelection();
	setModel(null);
	toc = null;
	nstack.clear();
	current = null;
	last = null;
	if (ma != null) {
	    removeMouseListener(ma);
	    ma = null;
	}
	entriesCompletedDone = false;
    }

    EventListenerList listenerList = new EventListenerList();
    ActionEvent actionEvent = null;

    /**
     * Add an action listener.
     * The source of an action event will be the entry that was
     * selected: an object whose type is {@link ObjTocPane.Entry}.
     * The action's command is the title for the entry.
     * @param l the action listener to add.
     */
    public void addActionListener(ActionListener l) {
	listenerList.add(ActionListener.class, l);
    }

    /**
     * Remove an action listener.
     * @param l the action listener to remove.
     */
    public void removeActionListener(ActionListener l) {
	listenerList.remove(ActionListener.class, l);
    }

    /**
     * Notify all action listeners of action-performed events.
     */
    protected void fireActionPerformed() {
	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	// Process the listeners last to first, notifying
	// those that are interested in this event
	actionEvent = new ActionEvent(selected, ActionEvent.ACTION_PERFORMED,
				      selected.title); 
	for (int i = listeners.length-2; i>=0; i-=2) {
	    if (listeners[i]==ActionListener.class) {
		((ActionListener)listeners[i+1]).actionPerformed(actionEvent);
	    }
	}
    }
    
    /**
     * Clear the selection.
     * This method also generates action events with the source set to
     * an entry with a null title and a null object.
     */
     public void clearSelection() {
	super.clearSelection();
	if (entriesCompletedDone) {
	    selected = deselected;
	    fireActionPerformed();
	}
    }

    private DefaultMutableTreeNode last = null;

    /**
     * Add a new node to the tree, giving it a name.
     *
     * @param name A string naming the node.
     * @param obj The object that represents the value of the node.
     * @throws IllegalStateException if this method was called after
     *         a call to <code>entriesCompleted</code>.
     * @see #entriesCompleted()
     * @see #nextLevel()
     * @see #prevLevel()
     */
    public void addEntry(String name, Object obj) {
	if (entriesCompletedDone) {
	    throw new IllegalStateException(errorMsg("entriesCompleted"));
	}
	DefaultMutableTreeNode node =
	    new DefaultMutableTreeNode(new Entry(this, name, obj));
	if (toc == null) {
	    toc = node;
	} else {
	    current.add(node);
	    last = node;
	}
    }

    /**
     * Add a new level to the tree rooted at the last node entered.
     * @throws IllegalStateException if this method was called after
     *         a call to <code>entriesCompleted</code>.
     * @see #prevLevel()
     * @see #addEntry(String, Object)
     */
    public void nextLevel() {
	if (entriesCompletedDone) {
	    throw new IllegalStateException(errorMsg("entriesCompleted"));
	}
	if (current == null) {
	    current = toc;
	    last = toc;
	} else {
	    nstack.push(current);
	    current = last;
	}
    }

    /**
     * Return to the previous level in the tree.
     * @throws IllegalStateException if this method was called after
     *         a call to <code>entriesCompleted</code> or if it was
     *         called more times than <code>nextLevel</code>
     * @see #nextLevel()
     * @see #entriesCompleted()
     * @see #addEntry(String, Object)
     */
    public void prevLevel() {
	if (entriesCompletedDone) {
	    throw new IllegalStateException(errorMsg("entriesCompleted"));
	}
	if (current == null) {
	    throw new IllegalStateException(errorMsg("atTopLevel"));
	}
	if (nstack.isEmpty()) {
	    current = null;
	} else {
	    current = (DefaultMutableTreeNode) nstack.pop();
	}
    }

    /**
     * Assert that no more entries will be added.
     * A call to this method will complete construction of
     * the tree.  It is equivalent to calling
     * <code>entriesCompleted(false)</code>.
     * @throws IllegalStateException if this method was already
     *         called.
     * @see #entriesCompleted(boolean)
     * @see #addEntry(String, Object)
     * @see #nextLevel()
     * @see #prevLevel()
     */
    public void entriesCompleted() {
	entriesCompleted(false);
    }
    private boolean entriesCompletedDone = false;
    private MouseAdapter ma = null;

    /**
     * Assert that no more entries will be added.
     * A call to this method will complete construction of
     * the tree.
     * @param expand <code>true</code> if all nodes should be
     *        expanded before the tree is displayed; false if 
     *        no nodes should be expanded
     * @throws IllegalStateException if this method was already
     *         called.
     * @see #addEntry(String, Object)
     * @see #nextLevel()
     * @see #prevLevel()
     */
    public void entriesCompleted(boolean expand) {
	if (entriesCompletedDone) {
	    throw new 
		IllegalStateException(errorMsg("multipleEntriesCompleted"));
	}
	setModel(new DefaultTreeModel(toc));
	getSelectionModel().setSelectionMode
	    (TreeSelectionModel.SINGLE_TREE_SELECTION);
	selected = (Entry)(toc.getUserObject());
	selected.mouseEvent = null;
	ma = new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
		    int clicks = e.getClickCount();
		    if (clicks == getToggleClickCount()) return;
		    int selRow = getRowForLocation(e.getX(), e.getY());
		    int button = e.getButton();
		    // We don't respond to mouse-wheel events at this point.
		    if (button == MouseEvent.MOUSE_WHEEL) return;
		    if (selRow != -1) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)
			    getPathForLocation(e.getX(), e.getY()).
			    getLastPathComponent();
			Entry entry = (Entry) node.getUserObject();
			selected = entry;
			selected.mouseEvent = e;
			fireActionPerformed();
		    } else {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)
			    getLastSelectedPathComponent();
			if (node == null) return;
			Entry entry = (Entry) node.getUserObject();
			if (entry != selected) {
			    selected = entry;
			    selected.mouseEvent = e;
			    fireActionPerformed();
			}
		    }
		}
	    };
	addMouseListener(ma);
	if (expand) {
	    // int n = getRowCount();
	    for (int i = 0; i < getRowCount(); i++) {
		expandRow(i);
	    }
	}
	entriesCompletedDone = true;
    }

    /**
     * Set the selection and perform an action.
     * This method should be called after the call to
     * <code>entriesCompleted</code>. It sets the selected object and calls
     * <code>fireActionPerformed()</code>.  Rows are indexed with 0
     * indicating the first visible node and with only visible nodes counted,
     * regardless of whether these nodes are displayable. Expanding
     * or collapsing nodes changes the indexing. Changing the portion of
     * the pane that is shown in the display does not change the indexing.
     * At the time this description was written, the JTree documentation was
     * somewhat obscure regarding how this indexing works.
     * @param row the row to select.
     * @throws IllegalStateException if this method  was called 
     *         before <code>entriesCompleted</code>.
     */
    public final void setSelectionWithAction(int row) {
	if (!entriesCompletedDone) {
	    throw new 
		IllegalStateException(errorMsg("initialSelectionErr"));
	}
	setSelectionRow(row);
	TreePath path = getPathForRow(row);

	DefaultMutableTreeNode node = (DefaultMutableTreeNode)
	    path.getLastPathComponent();
	selected = (Entry)node.getUserObject();
	selected.mouseEvent = null;
	fireActionPerformed();
    }

    /**
     * Constructor.
     * Initialization is handled by a series of method calls.
     * @see #addEntry(String, Object)
     * @see #nextLevel()
     * @see #prevLevel()
     * @see #entriesCompleted()
     * @see #entriesCompleted(boolean)
     * @see #setSelectionWithAction(int)
     */
    public ObjTocPane() {
	super();
    }
}
