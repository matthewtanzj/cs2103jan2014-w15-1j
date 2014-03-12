package storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import common.DateTimeTypeConverter;
import common.PandaLogger;

import core.Task;

public class StorageHelper {

	// A singleton class
	public static StorageHelper INSTANCE = new StorageHelper();

	protected static final String FILENAME = "data.json";
	private static final String ERROR_FILE_CREATION = "Error in file creation";
	private static final String ERROR_TASK_ADDITION = "Error in task addition";
	private static final String ERROR_FILE_IO = "Error in File IO";

	private Gson gson;
	private File file;

	private StorageHelper() {
		this.file = createOrGetFile(FILENAME);
		this.gson = new GsonBuilder()
				.registerTypeAdapter(DateTime.class,
						new DateTimeTypeConverter())
				.enableComplexMapKeySerialization().create();
	}

	public void writeTasks(List<Task> t) {
		PandaLogger.getLogger().info(
				"writeTasks: Length of Task array = " + t.size());
		JsonWriter writer;
		try {
			writer = new JsonWriter(new OutputStreamWriter(
					new FileOutputStream(this.file), "UTF-8"));
			if (t.size() == 0) {
				writer.beginArray();
				writer.endArray();
			} else {
				gson.toJson(t);
			}
		} catch (IOException e) {
			throw new Error(ERROR_TASK_ADDITION);
		}
	}

	// public ArrayList<Task> getAll() {
	// try(Reader reader = new
	// InputStreamReader(JsonToJava.class.getResourceAsStream(this.file),
	// "UTF-8")) {
	// Gson gson = new GsonBuilder().create();
	// } catch(IOException e) {
	// throw new Error(ERROR_FILE_IO);
	// }
	// }

	public ArrayList<Task> getAllTasks() {
		PandaLogger.getLogger().info("getAllTasks");
		ArrayList<Task> tasks = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(this.file));
			tasks = this.gson.fromJson(br, new TypeToken<List<Task>>() {
			}.getType());
			if(tasks == null) {
				return new ArrayList<Task>();
			}
			PandaLogger.getLogger().info(String.valueOf(tasks.size()));
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error(ERROR_FILE_IO);
		}
		return tasks;
	}

	private File createOrGetFile(String filename) {
		file = new File(filename);
		if (!file.isFile()) {
			try {
				file.createNewFile();
				writeTasks(new ArrayList<Task>());
			} catch (IOException e) {
				throw new Error(ERROR_FILE_CREATION);
			}
		}
		return file;
	}

	public void clearFile() {
		file.delete();
		this.file = createOrGetFile(FILENAME);
	}
}