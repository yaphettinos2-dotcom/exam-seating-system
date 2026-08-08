package examsystem.ui;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.util.List;

/**
 * Holds the widgets of one management tab (form fields, search box, table) so that
 * Departments, Students, Rooms and Schedules can share the same layout and behaviour.
 */
class CrudTab {
    final JPanel panel;
    final JTextField[] fields;
    final JTextField search;
    final DefaultTableModel model;
    final JTable table;
    /** Identifier of the record currently being edited; null means "creating a new record". */
    Object editingKey;

    CrudTab(JPanel panel, JTextField[] fields, JTextField search, DefaultTableModel model, JTable table) {
        this.panel = panel;
        this.fields = fields;
        this.search = search;
        this.model = model;
        this.table = table;
    }

    String text(int index) {
        return fields[index].getText().trim();
    }

    void setTexts(String... values) {
        for (int index = 0; index < fields.length; index++) {
            fields[index].setText(index < values.length && values[index] != null ? values[index] : "");
        }
    }

    void clear() {
        editingKey = null;
        setTexts();
    }

    /** Returns the value of the selected row at {@code column}, or null when nothing is selected. */
    Object selectedValue(int column) {
        int row = table.getSelectedRow();
        return row == -1 ? null : model.getValueAt(row, column);
    }

    void setRows(List<Object[]> rows) {
        model.setRowCount(0);
        rows.forEach(model::addRow);
    }
}
