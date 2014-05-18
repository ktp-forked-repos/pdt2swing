package pdt.gui.datapanels.handler;

import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;

import org.cs3.prolog.common.QueryUtils;
import org.cs3.prolog.pif.PrologInterfaceException;

import pdt.gui.data.PrologConnection;
import pdt.gui.datapanels.FactPanel;
import pdt.gui.datapanels.SpinnerWithCheckbox;
import pdt.gui.utils.PrologUtils;
import pdt.gui.utils.SimpleLogger;
import pdt.prolog.elements.PrologArgument;
import pdt.prolog.elements.PrologGoal;

public class PrologFactHandler extends PrologDataHandler<FactPanel> {

//	private FactPanel editPanel;
	private Set<PrologRelationHandler> relationHandler;
	private Map<String, Object> result;
	private String mainElementName;
	private final Map<String, ActionListener> additionalActions = new TreeMap<String, ActionListener>();
	
	public PrologFactHandler(PrologConnection con, String name, File outputFile, boolean isMainPredicate, PrologGoal goal) {
		super(con, name, outputFile, isMainPredicate, goal);
		relationHandler = new HashSet<>();
	}

	public PrologTextFileHandler createTextFileHandler(String title) {
		File textOutputDir = new File(outputFile.getParentFile(), getFunctor());
		PrologTextFileHandler textData = new PrologTextFileHandler(title, textOutputDir);
		return textData;
	}

	public void setMainElementName(String mainElementName) {
		this.mainElementName = mainElementName;
	}
	
	@Override
	public void showData() {
		SimpleLogger.debug(getQuery());
		try {
			result = pif.queryOnce(getQuery());
			result.put("ID", currentId);
			if (getEditPanel() != null) {
				getEditPanel().setData(result);
			}
		} catch (PrologInterfaceException e) {
			e.printStackTrace();
		}
	}

	public String getMainElement() {
		return getElementByName(mainElementName);
	}
	
	public String getElementByName(String elementName) {
		if (result != null && elementName != null) {
			Object element = result.get(elementName);
			if (element != null) {
				return element.toString();
			}
		}
		return null;
	}

	public boolean updateFromPanel(HashMap<String, JComponent> textFields) {
		if (currentId == null) {
			return false;
		}
		
		// check if name already exists (quick & dirty)
		// TODO: improve (move to prolog side)
		if (nameAlreadyExists(textFields)) {
			return false;
		}
		
		// get goal for assertion, use current id
		String goal = getGoalWithData(textFields, currentId);
		
		try {
			pif.queryOnce(QueryUtils.bT(UPDATE_FACT, goal));
		} catch (PrologInterfaceException e) {
			e.printStackTrace();
			return false;
		}
		
		updateVisualizer();
		updateRelationHandlers();
		return true;
	}

	private boolean nameAlreadyExists(HashMap<String, JComponent> textFields) {
		JComponent comp = textFields.get("Name");
		if (comp != null && comp instanceof JTextField) {
			String checkRename = checkRename((JTextField) comp);
			if (checkRename != null) {
				try {
					Map<String, Object> checkRes = pif.queryOnce(QueryUtils.bT("name_is_free", PrologUtils.quoteIfNecessary(checkRename)));
					if (checkRes == null) {
						// query failed --> name is not free
						JOptionPane.showMessageDialog(getEditPanel(), "Name \"" + checkRename + "\" existiert bereits.",  "Fehler", JOptionPane.ERROR_MESSAGE);
						return true;
					}
				} catch (PrologInterfaceException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	private String checkRename(JTextField tf) {
		String newName = tf.getText();
		String oldName = getElementByName("Name");
		if (newName.equals(oldName)) {
			return null;
		} else {
			return newName;
		}
	}

	public boolean saveAsNew(HashMap<String, JComponent> textFields) {
		// check if name already exists (quick & dirty)
		// TODO: improve (move to prolog side)
		if (nameAlreadyExists(textFields)) {
			return false;
		}

		// get goal for assertion, use empty ID
		String goal = getGoalWithData(textFields, "_");
		String id = null;
		try {
			Map<String, Object> result = pif.queryOnce(QueryUtils.bT(ADD_FACT, goal, "ID"));
			if (result.get("ID") != null) {
				id = result.get("ID").toString();
			}
		} catch (PrologInterfaceException e) {
			e.printStackTrace();
			return false;
		}
		
		updateVisualizer(id);
		updateRelationHandlers();
		return true;
	}
	
	public void delete() {
		if (currentId == null) {
			return;
		}
		
		String goal = getSimpleGoal();

		try {
			pif.queryOnce(QueryUtils.bT(REMOVE_FACT, goal));
			
			// if there is a text file, remove it also
			File dataDir = new File(outputFile.getParentFile(), getFunctor());
			File textFile = new File(dataDir, currentId);
			if (textFile.isFile()) {
				textFile.delete();
			}
			
			// same for image file
			File imgDir = new File(dataDir, "imgs");
			File imgFile = new File(imgDir, currentId + ".jpg");
			if (imgFile.isFile()) {
				imgFile.delete();
			}
			
		} catch (PrologInterfaceException e) {
			e.printStackTrace();
		}
		
		updateVisualizer();
	}

	
	private String getGoalWithData(HashMap<String, JComponent> textFields, String id) {
		String[] argNames = getArgNames();
		String[] assertArgs = new String[argNames.length];
		assertArgs[0] = id;
		
		for (int i=1; i<argNames.length; i++) {
			JComponent tf = textFields.get(argNames[i]);
			if (tf == null) {
				if (getArgs()[i-1].getType() == PrologArgument.NUMBER) {
					assertArgs[i] = "0";
				} else {
					assertArgs[i] = "''";
				}
			} else {
				String text = null;
				if (tf instanceof JTextField) {
					text = ((JTextField) tf).getText();
				} else if (tf instanceof JComboBox<?>) {
					text = ((JComboBox<?>) tf).getSelectedItem().toString();
				} else if (tf instanceof JSpinner) {
					text = ((JSpinner) tf).getValue().toString();
				} else if (tf instanceof SpinnerWithCheckbox) {
					text = ((SpinnerWithCheckbox) tf).getValue();
				} else if (tf instanceof JCheckBox) {
					if (((JCheckBox) tf).isSelected()) {
						text = "true";
					} else {
						text = "false";
					}
				}
				if (text.isEmpty()) {
					if (getArgs()[i].getType() == PrologArgument.NUMBER) {
						assertArgs[i] = "0";
					} else {
						assertArgs[i] = "''";
					}
				} else {
					if (tf instanceof SpinnerWithCheckbox) {
						assertArgs[i] = text;
					} else {
						assertArgs[i] = PrologUtils.quoteIfNecessary(text);
					}
				}
			}
		}
		
		return QueryUtils.bT(getFunctor(), (Object[]) assertArgs);
	}
	
	private String getSimpleGoal() {
		
		String[] retractArgs = new String[getArity()];
		retractArgs[0] = currentId;
		
		for (int i=1; i<getArity(); i++) {
			retractArgs[i] = "_";
		}
		
		return QueryUtils.bT(getFunctor(), (Object[]) retractArgs);
	}

	public void addAction(String actionName, ActionListener actionListener) {
		additionalActions.put(actionName, actionListener);
	}
	
	public Map<String, ActionListener> getAdditionalActions() {
		return additionalActions;
	}

	public PrologArgument getArgumentWithName(String name) {
		for (int i=0; i<getArgs().length; i++) {
			if (getArgs()[i].getName().equals(name)) {
				return getArgs()[i];
			}
		}
		return null;
	}

	public boolean isElementSelected() {
		return currentId != null;
	}
	
	public void addRelationHandler(PrologRelationHandler handler) {
		if (handler == null) {
			SimpleLogger.error("handler is null");
		} else {
			relationHandler.add(handler);
		}
	}
	
	public void removeRelationHandler(PrologRelationHandler handler) {
		if (handler == null) {
			SimpleLogger.error("handler is null");
		} else {
			relationHandler.remove(handler);
		}
	}
	
	public void updateRelationHandlers() {
		for (PrologRelationHandler h : relationHandler) {
			h.updateAutoCompletion();
		}
	}

}
