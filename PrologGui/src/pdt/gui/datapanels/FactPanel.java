package pdt.gui.datapanels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.cs3.prolog.connector.process.PrologProcess;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import pdt.gui.datapanels.handler.PrologFactHandler;
import pdt.prolog.elements.PrologArgument;
import pdt.prolog.elements.PrologFixedAtom;
import pdt.prolog.elements.PrologNumberRangeArgument;
import pdt.prolog.elements.PrologReferenceType;

public class FactPanel extends JPanel implements DataPanel {

	private static final long serialVersionUID = 1L;
	private final HashMap<String, JComponent> textFields = new HashMap<>();
	private HashMap<String, String> dummyValues = new HashMap<>();
	private JButton btUpdate;
	private JButton btDelete;
	private PrologFactHandler factHandler;

	/**
	 * Create the panel.
	 */
	public FactPanel(PrologFactHandler prolog) {
		factHandler = prolog;

		Map<String, ActionListener> additionalActions = prolog.getAdditionalActions();
		prolog.setEditPanel(this);
		String[] variables = prolog.getArgNames();
		
		int rowCount = 0;
		if (prolog.isMainPredicate()) {
			rowCount = variables.length + 3 + additionalActions.size()/2;
		} else {
			rowCount = variables.length + 2 + additionalActions.size()/2;
		}
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[rowCount];
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[rowCount];
		
		for (int i=0; i<=rowCount-1; i++) {
			gridBagLayout.rowHeights[i] = 0;
			gridBagLayout.rowWeights[i] = 0.0;
		}
		gridBagLayout.rowWeights[rowCount-1] = Double.MIN_VALUE;
		
		setLayout(gridBagLayout);
		
		for (int i=0; i<variables.length; i++) {
			// Label
			String s = variables[i];
			JLabel label = new JLabel(s);
			GridBagConstraints gbc_label = new GridBagConstraints();
			gbc_label.insets = new Insets(0, 0, 5, 5);
			gbc_label.anchor = GridBagConstraints.EAST;
			gbc_label.gridx = 0;
			gbc_label.gridy = i;
			add(label, gbc_label);
			
			PrologArgument arg = prolog.getArgumentWithName(s);
			if (arg == null) {
				System.out.println("null?");
			}
			
			JComponent component = null;
			
			if (arg instanceof PrologNumberRangeArgument) {
				PrologNumberRangeArgument numberArg = (PrologNumberRangeArgument) arg;
				SpinnerNumberModel model = new SpinnerNumberModel(numberArg.getLimitMin(), numberArg.getLimitMin(), numberArg.getLimitMax(), 1);
				if (numberArg.canBeUnsure()) {
					component = new SpinnerWithCheckbox(model);
				} else {
					component = new JSpinner(model);
				}
			} else if (arg instanceof PrologFixedAtom) {
				PrologFixedAtom fixedAtom = (PrologFixedAtom) arg;
		
				DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
				for(String value : fixedAtom.getValues()) {
					model.addElement(value);
				}
				component = new JComboBox<>(model);
			} else if (arg.getType() == PrologArgument.BOOLEAN) {
				component = new JCheckBox();
			} else {
				component = new JTextField();
				JTextField tf = (JTextField) component;
				tf.setColumns(10);
				
				if (arg instanceof PrologReferenceType) {
					String refType = ((PrologReferenceType) arg).getRefType();
					List<String> completionList = prolog.getAutoCompletionProvider().getCompletionList(refType);
					if (completionList != null) {
						AutoCompleteDecorator.decorate(tf, completionList, false);
					}
				}
			}
			
			if (arg.getType() == PrologArgument.ID || arg.getName().equalsIgnoreCase("id")) {
				component.setEnabled(false);
			}
			
			component.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent evt) {
					if (evt.getKeyCode() == KeyEvent.VK_S && evt.isControlDown()) {
						doUpdate();
					}
				}
			});
			GridBagConstraints gbc_component = new GridBagConstraints();
			gbc_component.insets = new Insets(0, 0, 5, 0);
			gbc_component.fill = GridBagConstraints.HORIZONTAL;
			gbc_component.gridx = 1;
			gbc_component.gridy = i;
			add(component, gbc_component);

			textFields.put(s, component);
		}
		updateDummyValues();
		
		btUpdate = new JButton("Update");
		btUpdate.setEnabled(false);
		btUpdate.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent evt) {
				doUpdate();
			}
		});
		GridBagConstraints gbc_btUpdate = new GridBagConstraints();
		gbc_btUpdate.insets = new Insets(0, 0, 5, 5);
		gbc_btUpdate.anchor = GridBagConstraints.EAST;
		gbc_btUpdate.gridx = 0;
		gbc_btUpdate.gridy = variables.length;
		add(btUpdate, gbc_btUpdate);
		

		int x = 1;
		int y = variables.length;
		if (prolog.isMainPredicate()) {
			JButton btSaveAsNew = new JButton("Save as New");
			btSaveAsNew.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent evt) {
					HashMap<String, String> dummyValuesBackup = new HashMap<>(dummyValues);
					updateDummyValues();
					if (!factHandler.saveAsNew(textFields)) {
						// reset dummy values
						dummyValues = dummyValuesBackup;
					}
				}
			});
			GridBagConstraints gbc_btSaveAsNew = new GridBagConstraints();
			gbc_btSaveAsNew.insets = new Insets(0, 0, 5, 0);
			gbc_btSaveAsNew.fill = GridBagConstraints.HORIZONTAL;
			gbc_btSaveAsNew.gridx = 1;
			gbc_btSaveAsNew.gridy = variables.length;
			add(btSaveAsNew, gbc_btSaveAsNew);
			
			btDelete = new JButton("Delete");
			btDelete.setEnabled(false);
			btDelete.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent evt) {
					if (factHandler.isElementSelected()) {
						int answer = JOptionPane.showConfirmDialog(FactPanel.this, "Soll der Eintrag wirklich gel�scht werden?", "Eintrag l�schen", JOptionPane.YES_NO_OPTION);
						if (answer == JOptionPane.YES_OPTION) {
							updateDummyValues();
							factHandler.delete();
						}
					}
				}
			});
			GridBagConstraints gbc_btDelete = new GridBagConstraints();
			gbc_btDelete.insets = new Insets(0, 0, 5, 5);
			gbc_btDelete.anchor = GridBagConstraints.EAST;
			gbc_btDelete.gridx = 0;
			gbc_btDelete.gridy = variables.length+1;
			add(btDelete, gbc_btDelete);
			
			y++;
		}
		
		
		for( Map.Entry<String, ActionListener> entry : additionalActions.entrySet() ) {
			JButton button = new JButton(entry.getKey());
			button.addActionListener(entry.getValue());
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(0, 0, 5, 5);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridx = x;
			gbc.gridy = y;
			add(button, gbc);
			
			if (x == 1) {
				x = 0;
				y++;
			} else {
				x = 1;
			}
		}
		
	}

	protected void updateDummyValues() {
		for(String key : textFields.keySet()) {
			String value = getSingleEntry(key);
			dummyValues.put(key, value);
		}
	}

	public void setData(Map<String, Object> result) {
		if (result != null) {
			for(String s : textFields.keySet()) {
				String value = result.get(s).toString();
				String settedValue = setSingleEntry(s, value);
				dummyValues.put(s, settedValue);
			}
			updateButtons(true);
		}
	}
	
	public String setSingleEntry(String key, String value) {
		JComponent tf = textFields.get(key);
		String setValue = value;
		if (setValue == null) {
			setValue = "";
		}
		
		if (tf instanceof JTextField) {
			setValue = factHandler.getDisplayString(key, value);
			((JTextField) tf).setText(setValue);
		} else if (tf instanceof JSpinner) {
			((JSpinner) tf).setValue(Integer.parseInt(setValue));
		} else if (tf instanceof SpinnerWithCheckbox) {
			((SpinnerWithCheckbox) tf).setValue(setValue);
		} else if (tf instanceof JComboBox<?>) {
			JComboBox<?> comboBox = (JComboBox<?>) tf;
			int index = ((DefaultComboBoxModel<?>) comboBox.getModel()).getIndexOf(setValue);
			if (index == -1) {
				comboBox.setSelectedIndex(0);
			} else {
				comboBox.setSelectedItem(setValue);
			}
		} else if (tf instanceof JCheckBox) {
			((JCheckBox) tf).setSelected(setValue.equalsIgnoreCase("true"));
		}
		return setValue;
	}
	
	private void clearSingleEntry(String key) {
		JComponent tf = textFields.get(key);
		
		if (tf instanceof JTextField) {
			((JTextField) tf).setText("");
		} else if (tf instanceof JSpinner) {
			((JSpinner) tf).setValue(0);
		} else if (tf instanceof SpinnerWithCheckbox) {
			((SpinnerWithCheckbox) tf).setValue("u(0)");
		} else if (tf instanceof JComboBox<?>) {
			((JComboBox<?>) tf).setSelectedIndex(0);
		} else if (tf instanceof JCheckBox) {
			((JCheckBox) tf).setSelected(false);
		}
	}
	
	public String getSingleEntry(String key) {
		JComponent tf = textFields.get(key);
		String result = null;
		if (tf != null) {
			if (tf instanceof JTextField) {
				result = ((JTextField) tf).getText();
			} else if (tf instanceof JSpinner) {
				result = ((JSpinner) tf).getValue().toString();
			} else if (tf instanceof SpinnerWithCheckbox) {
				result = ((SpinnerWithCheckbox) tf).getValue();
			} else if (tf instanceof JComboBox<?>) {
				result = ((JComboBox<?>) tf).getSelectedItem().toString();
			} else if (tf instanceof JCheckBox) {
				if (((JCheckBox) tf).isSelected()) {
					result = "true";
				} else {
					result = "false";
				}
			}
		}
		return result;
	}

	@Override
	public void clearPanel() {
		for(String s : textFields.keySet()) {
			clearSingleEntry(s);
		}
		updateDummyValues();
		updateButtons(false);
	}
	
	public void updateButtons(boolean enabled) {
		btUpdate.setEnabled(enabled);
		
		// btDelete might be null if predicate is not main 
		if (btDelete != null) {
			btDelete.setEnabled(enabled);
		}
	}

	@Override
	public boolean changed() {
		for(String key : textFields.keySet()) {
			String value1 = getSingleEntry(key);
			String value2 = dummyValues.get(key);
			if (!value1.equals(value2)) {
				return true;
			}
		}
		return false;
	}
	
	public PrologProcess getPrologProcess() {
		return factHandler.getPrologProcess();
	}

	private void doUpdate() {
		HashMap<String, String> dummyValuesBackup = new HashMap<>(dummyValues);
		updateDummyValues();
		if (!factHandler.updateFromPanel(textFields)) {
			// reset dummy values
			dummyValues = dummyValuesBackup;
		}
	}

}
