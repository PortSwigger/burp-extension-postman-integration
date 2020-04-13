package assess;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

public class PostmanTableModel extends AbstractTableModel implements TableModelListener {

	private static final long serialVersionUID = 5767402190172442585L;

	/** Table Date */
	private ArrayList<ArrayList<String>> tableData;
	
	/** Table Column Name */
	private static final String[] columnNames = {"Folder", "Name", "Method", "URL"};
	
	/** Table Row Count */
	public int tableRowCount;
	
	/** Table Column Count */
	public static final int TABLE_COLUMN_COUNT = 4;
	
	/** Table column index */
	public static final int FOLDER_NAME_INDEX = 0;
	public static final int NAME_COLUMN_INDEX = 1;
	public static final int METHOD_COLUMN_INDEX = 2;
	public static final int URL_COLUMN_INDEX = 3;
	
	public PostmanTableModel(int tableRowCount) {
		this.tableRowCount = tableRowCount;
		tableData = new ArrayList<>();
		for(int i = 0; i < tableRowCount; i++) {
			ArrayList<String> col = new ArrayList<>();
			for(int j = 0; j < TABLE_COLUMN_COUNT; j++) {
				col.add("");
			}
			tableData.add(col);
		}

		// add listener
		addTableModelListener(this);
	}
	
	public ArrayList<ArrayList<String>> getTabledata() {
		return this.tableData;
	}
 			
	@Override
	public int getColumnCount() {
		return TABLE_COLUMN_COUNT;
	}

	@Override
	public int getRowCount() {
		return tableData.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return tableData.get(rowIndex).get(columnIndex);
	}

	@Override
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return getValueAt(0, columnIndex).getClass();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if(columnIndex == NAME_COLUMN_INDEX || columnIndex == FOLDER_NAME_INDEX)
			return true;
		else 
			return false;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		tableData.get(rowIndex).set(columnIndex, aValue.toString());
		fireTableDataChanged();
	}

	public void removeRow(int rowIndex) {
		tableData.remove(rowIndex);
		fireTableRowsDeleted(rowIndex, rowIndex);
	}

	//
	// implement TableModelListener
	//
	@Override
	public void tableChanged(TableModelEvent e) {
	}
}