package logic;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;

import storage.StorageHelper;
import storage.UndoStorage;

import common.PandaLogger;

import core.Task;

public class CommandFactory {

	// EDIT//
	private static final String MESSAGE_INVALID_EDIT = "Usage: edit <index> <description> on <date> from <start time> to <end time>";
	private static final String MESSAGE_INVALID_NUMBERFORMAT = "Please key in an integer";
	private static final String MESSAGE_INVALID_NUMBERSIGN = "Please key in a positive number";
	private static final String MESSAGE_EMPTY = "file is empty";

	private static Integer NUMBER_TASK_INDEX = 0;
	private static Integer EDIT_OFFSET = 1;
	private static Integer OFFSET = 1;

	private static String FEEDBACK;
	String userInputDesc;
	String commandType;
	String[] inputArray;

	// DELETE//
	private static final String MESSAGE_INVALID_DELETE = "Usage: delete <number>";
	private static final String MESSAGE_INVALID_NUMBER = "Please choose another value";
	private static final String MESSAGE_DELETED = "deleted : \"%s\"";

	private static Integer DELETE_PARA = 1;
	private static Integer DELETE_OFFSET = 1;

	public static CommandFactory INSTANCE = new CommandFactory();
	public final String UNDO_ADD = "add";
	public final String UNDO_EDIT = "edit";
	public final String UNDO_DONE = "done";
	public final String UNDO_ARCHIVE = "archive";
	public final String UNDO_DONEALL = "doneall";
	public final String UNDO_ARCHIVEALL = "archiveall";

	private List<Task> tasks;
	private LinkedHashMap<Integer, Integer> tasksMap;

	private StorageHelper storage;
	private UndoStorage undoStorage;
	private Logger logger = PandaLogger.getLogger();

	private Stack<SimpleEntry<Integer, Command>> undoStack;

	private CommandFactory() {
		this.tasks = new ArrayList<Task>();
		this.undoStack = new Stack<SimpleEntry<Integer, Command>>();
		this.tasksMap = new LinkedHashMap<Integer, Integer>(); // <ID to display, real ID in tasks>
		this.storage = StorageHelper.INSTANCE;
		this.undoStorage = UndoStorage.INSTANCE;
		this.fetch();
	}
	
	/* populate tasks buffer and undo command stack */ 
	private void fetch() {
		this.tasks = this.storage.getAllTasks();
		this.populateTasksMapWithDefaultCriteria();
		this.populateUndoStack();
	}

	// initialize and populate undoStack
	private void populateUndoStack() {
		this.undoStack = this.undoStorage.getAllCommands();
	}
	
	/* by default, display tasks which are not marked as deleted */
	private void populateTasksMapWithDefaultCriteria() {
		ArrayList<Integer> undeletedTasksIDs = Criteria.getAllUndeletedTasks(tasks);
		for(int i = 0; i < undeletedTasksIDs.size(); i++) {
			this.tasksMap.put(i, undeletedTasksIDs.get(i));
		}
	}

	public List<Task> getTasks() {
		return this.tasks;
	}
	
	public LinkedHashMap<Integer, Integer> getTasksMap() {
		return this.tasksMap;
	}

	public void process(Command command) {
		executeCommand(command);
	}

	public void executeCommand(Command command) {
		assert (command.rawText != null);
		switch (command.command) {
		case ADD:
			doAdd(command);
			break;
		case LIST:
			doList(command);
			break;
		case EDIT:
			doEdit(command);
			break;
		case UNDO:
			doUndo();
			break;
		case ARCHIVE:
			break;
		case CLEAR:
			break;
		case DONEALL:
			break;
		case ARCHIVEALL:
			break;
		case DELETE:
			doDelete(command);
			break;
		case HELP:
			break;
		default:
			break;
		}
	}

	private void doUndo() {
		logger.info("doUndo");
		SimpleEntry<Integer, Command> lastEntry = this.undoStack.pop();
		int taskid = lastEntry.getKey();
		Command lastCommand = lastEntry.getValue();
		logger.info("Last Command:" + lastCommand.toString());
		executeUndo(taskid, lastCommand);
		syncTasks();
	}

	private void executeUndo(int taskid, Command command) {
		assert (command.rawText == null);
		switch (command.command) {
		case ADD:
			doUndoAdd(taskid, command);
			break;
		case EDIT:
			doUndoEdit(taskid, command);
			break;
		case DELETE:
			doUndoDelete(taskid, command);
			break;
		default:
			return;
		}
	}

	private void doAdd(Command command) {
		assert (command.rawText!=null);
		Task newTask = new Task(command.rawText);
		this.tasks.add(newTask);
		this.populateTasksMapWithDefaultCriteria();		// regenerate the TaskMap
		this.undoStack.push(new SimpleEntry<Integer, Command>(this.tasks.size() - OFFSET, command));
		syncTasks();
	}
	
	private void doUndoAdd(int taskid, Command command) {
		
	}

	private void doList(Command command) {
		logger.info("doList");
//		this.populateTasksMapWithDefaultCriteria();
	}

	/* remove the original task from tasksMap and replace it with new task */
	private void doEdit(Command command) {
		String userInput = command.rawText;
		assert (userInput != null);
		this.logger.info("doEdit:" + userInput);
		if (checkEditIndexInput(userInput)) {
			int taskInt = (Integer.parseInt(getFirstWord(userInput)) - EDIT_OFFSET);
			Task editTask = new Task(obtainUserEditInput(userInput));
			this.tasks.set(tasksMap.get(taskInt), editTask);
			syncTasks();
		}
		
		// push the command with the id
	}
	
	private void doUndoEdit(int taskid, Command command) {
		
	}
	
	private void doDelete(Command command) {
		String inputNumber = command.rawText;
		assert (inputNumber != null);
		this.logger.info("doDelete:" + inputNumber);
		if (checkDeleteInput(inputNumber)) {
			int inputIndex = Integer.parseInt(inputNumber);
			inputIndex = tasksMap.get(inputIndex-1);			// get the actual index
			tasks.get(inputIndex).setMarkAsDelete();
			this.populateTasksMapWithDefaultCriteria();
			syncTasks();
		}
		
		// push the command with the id
	}
	
	private void doUndoDelete(int taskid, Command command) {
		
	}

	// Method to check delete parameter
	private boolean checkDeleteInput(String inputNumber) {
		// No argument input
		if (!isValidString(inputNumber)) {
			showToUser(MESSAGE_INVALID_DELETE);
			FEEDBACK = MESSAGE_INVALID_DELETE;
			return false;
		}
		/*
		 * Checks if argument fulfill the delete parameters, is a positive non
		 * zero integer and whether the number specified is within the array
		 * size
		 */
		String[] stringArray = inputNumber.split(" ");
		if (stringArray.length != DELETE_PARA) {
			showToUser(MESSAGE_INVALID_DELETE);
			FEEDBACK = MESSAGE_INVALID_DELETE;
			return false;
		} else if (!isPositiveNonZeroInt(inputNumber)) {
			return false;
		} else if (!checkIfNumberBelowArraySize(inputNumber)) {
			return false;
		}
		return true;
	}

	private boolean checkEditIndexInput(String inputNumber) {
		this.logger.info("checkEditIndexInput:" + inputNumber);
		String[] stringArray = inputNumber.split(" ");
		String taskIndex = stringArray[NUMBER_TASK_INDEX];
		// No argument input
		if (!isValidString(inputNumber)) {
			showToUser("here valid string");
			showToUser(MESSAGE_INVALID_EDIT);
			FEEDBACK = MESSAGE_INVALID_EDIT;
			return false;
		}
		/*
		 * Checks if argument fulfill the delete parameters, is a positive non
		 * zero integer and whether the number specified is within the array
		 * size
		 */
		else if (!isPositiveNonZeroInt(taskIndex)) {
			return false;
		} else if (!checkIfNumberBelowArraySize(taskIndex)) {
			return false;
		}
		return true;
	}

	private boolean checkIfNumberBelowArraySize(String n) {
		if (checkIfFileIsEmpty()) {
			showToUser(MESSAGE_EMPTY);
			FEEDBACK = MESSAGE_EMPTY;
			return false;
		}
		try {
			int num = Integer.parseInt(n) - 1;
			if (num >= tasks.size()) {
				showToUser(MESSAGE_INVALID_NUMBER);
				FEEDBACK = MESSAGE_INVALID_NUMBER;
				return false;
			}
			return true;
		} catch (NumberFormatException nfe) {
			showToUser(MESSAGE_INVALID_NUMBERFORMAT);
			FEEDBACK = MESSAGE_INVALID_NUMBERFORMAT;
			return false;
		}
	}

	// Method to check for number >0
	private boolean isPositiveNonZeroInt(String indexNumber) {
		try {
			int i = Integer.parseInt(indexNumber);
			if (i > 0) {
				return true;
			} else {
				showToUser(MESSAGE_INVALID_NUMBERSIGN);
				FEEDBACK = MESSAGE_INVALID_NUMBERSIGN;
				return false;
			}
		} catch (NumberFormatException nfe) {
			showToUser(MESSAGE_INVALID_NUMBERFORMAT);
			FEEDBACK = MESSAGE_INVALID_NUMBERFORMAT;
			return false;
		}
	}

	// remove task index from usercommand and return edit input
	private String obtainUserEditInput(String userCommand) {
		StringBuilder sb = new StringBuilder();
		String[] stringArray = userCommand.split(" ");
		for (int i = 1; i < (stringArray.length); i++) {
			sb.append(stringArray[i]);
			sb.append(" ");
		}
		showToUser(sb.toString());
		this.logger.info("obtainUserEditInput:" + sb.toString());
		return sb.toString();
	}

	private static String getFirstWord(String userCommand) {
		String commandTypeString = userCommand.trim().split("\\s+")[0];
		return commandTypeString;
	}

	// Method checks if inputString is a valid String
	private boolean isValidString(String inputString) {
		if (inputString.trim().length() == 0) {
			return false;
		}
		return true;
	}

	// Method checks if data list is empty
	private boolean checkIfFileIsEmpty() {
		if (tasks.isEmpty()) {
			return true;
		}
		return false;
	}

	private void showToUser(String outputString) {
		System.out.println(outputString);
	}

	public String testEdit(String userInput) {
		tasks.clear();
		Task task1 = new Task("meeting1 on 27-2-2014 from 1pm to 2pm");
		tasks.add(task1);
		this.populateTasksMapWithDefaultCriteria();
		if (checkEditIndexInput(userInput)) {
			int taskInt = (Integer.parseInt(getFirstWord(userInput)) -EDIT_OFFSET);
			Task editTask = new Task(obtainUserEditInput(userInput));
			int inputIndex = tasksMap.get(taskInt);
			this.tasks.set(inputIndex, editTask);
			syncTasks();
			StringBuilder sb = new StringBuilder();
			sb.append(tasks.get(inputIndex).getTaskDescription());
			sb.append(tasks.get(inputIndex).getTaskStartTime().getHourOfDay());
			sb.append(tasks.get(inputIndex).getTaskEndTime().getHourOfDay());
			return sb.toString();
		} else {
			return FEEDBACK;
		}
	}

	public String testDelete(String inputNumber) {
		tasks.clear();
		Task task1 = new Task("meeting1 on 27-2-2014 from 1pm to 2pm");
		Task task2 = new Task("meeting2 on 27-2-2014 from 2pm to 3pm");
		Task task3 = new Task("meeting3 on 27-2-2014 from 3pm to 4pm");
		tasks.add(task1);
		tasks.add(task2);
		tasks.add(task3);
		this.populateTasksMapWithDefaultCriteria();
		if (checkDeleteInput(inputNumber)) {
			if (checkDeleteInput(inputNumber)) {
				int inputIndex = Integer.parseInt(inputNumber);
				inputIndex = tasksMap.get(inputIndex-1);			// get the actual index
				tasks.get(inputIndex).setMarkAsDelete();
				this.populateTasksMapWithDefaultCriteria();
				syncTasks();
			}
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < tasks.size(); i++) {
				if (tasks.get(i).getMarkAsDelete()) {
					int index = i + 1;
					sb.append("task" + index + "deleted");
				} else {
					int index = i + 1;
					sb.append("task" + index + "notdeleted");
				}
			}
			return sb.toString();
		} else {
			return FEEDBACK;
		}
	}

	private void syncTasks() {
		this.storage.writeTasks(tasks);
	}
}
