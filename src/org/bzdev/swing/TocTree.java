package org.bzdev.swing;
import java.awt.event.ActionListener;

/**
 * Interface for Table of Contents.
 * A table of contents is a hierarchical mapping between a series
 * of names and and a set of objects.  The hierarchy is a tree.
 * The method <code>addEntry</code> should be called to create the
 * root of a tree, followed by a call to <code>nextLevel</code> to
 * add children to the tree.  Each call to <code>nextLevel</code>
 * must be balanced by a call to <code>prevLevel</code>.  A call to
 * the method <code>entriesCompleted</code> indicates that the
 * table of contents is complete.  One may select particular rows
 * by calling <code>setSelectionWithAction</code>, whose argument
 * is an index into the displayable nodes in the tree, indexed by
 * the order of entry. See {@link javax.swing.JTree} for a definition of
 * displayable versus visible.  A call to <code>clearSelection</code>
 * clears the current selection, and a call to <code>clearToc()</code>
 * clears the contents and puts the object in a state where a new
 * set of entries can be added.
 *<p>
 * The interface also supports action listeners, using the methods
 * <code>addActionListener</code> and <code>removeActionListener</code>.
 * The source of an action listener is an instance of the class
 * <code> TocEntry</code>.
 *
 * @author Bill Zaumen
 * @version $Revision: 1.4 $, $Date: 2005/05/26 14:53:33 $
 */
public interface TocTree {

    /**
     * Add an action listener.
     * @param l the action listener to add.
     */
    public void addActionListener(ActionListener l);

    /**
     * Remove an action listener.
     * @param l the action listener to remove.
     */
    public void removeActionListener(ActionListener l);

    /**
     * Clear the selection.
     * This method also generates action events with the source set to
     * an entry with a null title and a null object.
     */
    public void clearSelection();

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
    public void addEntry(String name, Object obj);


    /**
     * Add a new level to the tree rooted at the last node entered.
     * @throws IllegalStateException if this method was called after
     *         a call to <code>entriedCompleted</code>.
     * @see #prevLevel()
     * @see #addEntry(String, Object)
     */
    public void nextLevel();

    /**
     * Return to the previous level in the tree.
     * @throws IllegalStateException if this method was called after
     *         a call to <code>entriesCompleted</code> or if it was
     *         called more times than <code>nextLevel</code>
     * @see #nextLevel()
     * @see #entriesCompleted()
     * @see #addEntry(String, Object)
     */
    public void prevLevel();

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
    public void entriesCompleted();

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
    public void entriesCompleted(boolean expand);


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
    public void setSelectionWithAction(int row);

    /**
     * Ensures that the node in the specified row is collapsed.
     * If row is &lt; 0 or &ge; getRowCount this will have no effect.
     * @param row an integer specifying a row, where 0 is the first
     *        viewable row.  
     */
    public void collapseRow(int row);

    /**
     * Ensures that the node in the specified row is expanded and viewable.
     * If row is &lt; 0 or &ge; getRowCount this will have no effect.
     * @param row an integer specifying a display row, where 0 is the 
     *       first viewable  row.
     */
    public void expandRow(int row);

    /**
     * Returns true if the node at the specified display row is collapsed.
     * @param row the row to check, where 0 is the first viewable row.
     * @return true if the node is currently collapsed, otherwise false
     *
     */
    public boolean isCollapsed(int row);

    /**
     * Returns true if the node at the specified display row is currently 
     * expanded.
     * @param row he row to check, where 0 is the first viewable row.
     * @return true if the node is currently expanded, otherwise false
     */
    public boolean isExpanded (int row);

    /**
     * Clear the table of contents and puts it in a state where entries
     * can be added.
     */
    public void clearToc();
}
