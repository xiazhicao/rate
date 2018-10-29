package Optimized;

import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.rate.cache.CacheKey;
import com.rate.cache.CacheManager;
import com.rate.configuration.CFactory;
import com.rate.context.DataSource;
import com.rate.persistence.ConnectionManager;
import com.rate.persistence.ValueObject;
import com.rate.persistence.ValueObjectFactory;
import com.rate.persistence.helpers.Attribute;
import com.rate.persistence.helpers.FinderMethod;
import com.rate.persistence.helpers.StoredProcedure;
import com.rate.persistence.validator.BasePersistenceValidator;
import com.rate.persistence.valueobject.GenericPersistenceStrategy;
import com.rate.persistence.valueobject.ValueObjectConfiguration;
import com.rate.persistence.valueobject.ValueObjectConfigurationReader;
import com.rate.persistence.valueobject.ValueObjectList;
import com.rate.persistence.valueobject.ValueObjectListGenerator;
import com.rate.xml.XMLUtils;

public class ReadXMLFile {

	private final static int TYPE_MAP_SIZE = 101;
	protected final static String BOOLEAN_CHAR_REP_FALSE = "0";
	protected final static String BOOLEAN_CHAR_REP_TRUE = "1";
	private final transient static int[] class2SQLTypeMap = new int[TYPE_MAP_SIZE];

	private static final int AUTO_BOX_CACHE_MAX = 10000;
	private static long intsAllocated = 0;
	private static long intsAllocatedZeroTo10K = 0;
	private static long intsAllocated10KTo20K = 0;
	private static long intsAllocated20KTo100K = 0;
	private static long intsAllocatedOver100K = 0;
	private static long intsAllocatedCached = 0;

	public ValueObjectList findListByCriteria(Connection con, String typeName, Vector criteria, String finderName,
			boolean useCache, byte scope, String strategyClassName) throws Exception, Exception {
		ValueObjectList list = null;

		if (list == null) {
			ValueObjectConfiguration config = getObjectConfiguration(typeName);
			GenericPersistenceStrategy strategy = null;
			if (strategyClassName == null) {
				strategy = ValueObject.getDefaultStrategy();
			} else {
				strategy = createStrategy(strategyClassName);
			}

			list = strategy.loadList(con, config, criteria, finderName);
		}

		if (list != null && !list.isEmpty()) {
			for (Object o : list) {
				ValueObject vo = (ValueObject) o;

				if (vo != null && vo.getConfiguration() != null) {
					ArrayList<String> validators = vo.getConfiguration().getValidatorList();
					for (String validatorName : validators) {
						BasePersistenceValidator validator = BasePersistenceValidator.getValidator(validatorName);
						if (validator != null && !validator.validate(vo)) {
							Exception pve = new Exception("Persistence Security Violation: " + vo.getClass().getName()
									+ " using " + finderName);
							pve.printStackTrace();

							throw pve;
						}
					}
				}
			}
		}
		return list;
	}

	public ValueObject findObjectByCriteria(String typeName, Vector criteria, String finderName,
			String strategyClassName, boolean useCache) throws Exception, Exception {
		ValueObject obj = null;

		ValueObjectConfiguration config = getObjectConfiguration(typeName);
		if (strategyClassName == null) {
			strategyClassName = ValueObject.DEFAULT_STRATEGY_CLASS_NAME;
		}

		GenericPersistenceStrategy strategy = createStrategy(strategyClassName);
		obj = loadObject(null, config, criteria, finderName);

		if (obj != null && obj.getConfiguration() != null) {
			ArrayList<String> validators = obj.getConfiguration().getValidatorList();
			for (String validatorName : validators) {
				BasePersistenceValidator validator = BasePersistenceValidator.getValidator(validatorName);
				if (validator != null && !validator.validate(obj)) {
					Exception pve = new Exception("Persistence Security Violation: " + this.getClass().getName() + "["
							+ (obj.getId() != null ? obj.getId() : "") + "] using " + finderName);
					pve.printStackTrace();

					throw pve;
				}
			}
		}

		return obj;
	}

	public ValueObject loadObject(Connection con, ValueObjectConfiguration config, Vector keys, String methodName)
			throws Exception {
		String msg = "StoredProcedurePersistence.loadObject(";
		msg += config.getTypeName() + ", " + methodName + ")";
		FinderMethod finder = config.getFinderMethod(methodName);
		StoredProcedure sp = finder.getStoredProcedure();
		ValueObject newOb = null;
		ValueObject parentOb = null;
		CallableStatement statement = null;

		long startTime = System.currentTimeMillis();

		ValueObjectConfiguration childConf = config;

		try {
			String sqlStatement = createStatement(sp);

			statement = initializeStatement(con, sp, sqlStatement, keys);

			boolean hasResultSet = statement.execute();
			displaySQLWarnings(statement);

			boolean moreResults = true;
			int i = 0;

			List<ValueObject> cacheList = new ArrayList<ValueObject>();

			while (hasResultSet && moreResults) {
				ResultSet rs = statement.getResultSet();

				// we're iterating through the ResultSets
				// each subsequent Set will be a child object of the parent object
				if (i > 0) {
					Vector<ValueObjectConfiguration> resultConfigs = sp.getResultConfigs();
					// In case the database has returned more resultsets then the config is aware
					if (resultConfigs.size() > i) {
						childConf = resultConfigs.get(i);
					} else {
						moreResults = statement.getMoreResults();
						continue;
					}
				}

				List<ValueObject> list = readResultSet(rs, childConf, null);

				// if we get more than a single row here throw up an exception
				// because we were expecting a unique value
				// if (list.size() > 1)
				if (list.size() > 0) {
					newOb = list.get(0);
				} else {
					newOb = null;
				}

				// save the parent object
				if (i == 0) {
					parentOb = newOb;

					if (parentOb == null) {
						// no base object matching our criteria was returned:
						// get out of the processing loop without worrying about any other resultSets
						break;
					}

					if (childConf.useCache()) {
						cacheList.add(parentOb);
					}
				} else {
					// assign the child object to where it belongs in the parent
					Attribute attr = (Attribute) sp.getResults().get(i);
					String colName = attr.getColumnName();
					boolean isCollection = attr.getCollection();

					if (parentOb != null) {
						if (isCollection) {
							// convert List<ValueObject> to ValueObjectList
							ValueObjectList voList = new ValueObjectList();
							voList.addAll(list);
							setObjectValue(parentOb, colName, voList);
							if (childConf.useCache()) {
								cacheList.addAll(list);
							}
						} else if (newOb != null) {
							setObjectValue(parentOb, colName, newOb);
							if (childConf.useCache()) {
								cacheList.add(newOb);
							}
						}
					}
				}

				moreResults = statement.getMoreResults();

				i++;
			}
		} catch (SQLException se) {
			StringBuffer spText = new StringBuffer();
			spText.append(sp.getName() + "(");

			for (int i = 0; i < keys.size(); i++) {
				Object obj = keys.get(i);

				spText.append(obj);

				if (i + 1 < keys.size()) {
					spText.append(",");
				}
			}

			spText.append(")");

			StringBuffer s = new StringBuffer();
			if (se.getSQLState() != null && se.getSQLState().equalsIgnoreCase("42815") && se.getErrorCode() == -4461)
				s.append("TYPE 4 driver issue - ");
			s.append("Error in StoredProcedurePersistence.loadObject - " + spText.toString());

		} finally {
			closeStatement((con == null), statement);
		}

		long duration = System.currentTimeMillis() - startTime;
		displayProcedureTiming(sp.getName(), duration);

		// object is in sync with DB
		if (parentOb != null) {
			parentOb.setIsModified(false);
		}

		return parentOb;
	}

	private static GenericPersistenceStrategy strategy = null;

	public GenericPersistenceStrategy createStrategy(String className) throws Exception {
		try {
			Class type = Class.forName(className);
			strategy = (GenericPersistenceStrategy) type.newInstance();
		} catch (ClassNotFoundException cnfe) {
			throw new Exception("Cannot create strategy: " + cnfe.toString());
		} catch (IllegalAccessException iae) {
			throw new Exception("Cannot create strategy: " + iae.toString());
		} catch (InstantiationException ie) {
			throw new Exception("Cannot create strategy: " + ie.toString());
		}

		return strategy;
	}

	public ValueObjectConfiguration getObjectConfiguration(String className) throws Exception {
		try {
			Class type = Class.forName(className);

			return getObjectConfiguration(type);
		} catch (ClassNotFoundException cnfe) {
			throw new Exception("Cannot find class " + className + ": " + cnfe.toString());
		}
	}

	public ValueObjectConfiguration getObjectConfiguration(Class type) throws Exception {
		ValueObjectConfiguration config = null;

		String typeName = type.getName();

		// otherwise read it from disk then cache it
		try {
			ValueObjectConfigurationReader reader = new ValueObjectConfigurationReader();

			config = reader.readConfiguration(type);
		} catch (Exception xce) {
			System.out.println("Configuration problem with class " + typeName + ": " + xce.toString());
		}

		if (config != null) {
			config.setType(type);
		}

		return config;
	}

	public ValueObjectConfiguration readConfiguration(Object valueOb) throws Exception {
		InputSource source = XMLUtils.getConfigInputSource(valueOb);
		XMLReader reader = XMLUtils.getXMLReader();
		ValueObjectConfiguration config;

		if (valueOb instanceof Class) {
			config = new ValueObjectConfiguration((Class) valueOb);
		} else {
			config = new ValueObjectConfiguration(valueOb.getClass());
		}

		try {
			reader.parse(source);
		} catch (SAXParseException spe) {
			spe.printStackTrace();

			String err = "File " + XMLUtils.getConfigFileName(valueOb) + " Line " + spe.getLineNumber() + ", Column "
					+ spe.getColumnNumber();

			throw new Exception(err);
		} catch (SAXException se) {

			throw new Exception(se.toString());
		} catch (IOException ioe) {

			throw new Exception(ioe.toString());
		}

		return config;
	}

	public static InputSource getConfigInputSource(Object forWhom) throws Exception {
		// this is based on the class of the passed-in object
		if (forWhom == null) {
			return null; // we can't handle a null object
		}

		Class inClass = null;

		if (forWhom instanceof java.lang.Class) {
			inClass = (Class) forWhom;
		} else {
			inClass = forWhom.getClass();
		}

		if (inClass == null) {
			return null;
		}

		// we need to calculate inClass's package name
		String className = inClass.getName();
		String pkgName = inClass.getPackage().getName();

		String fileName = className.substring(pkgName.length() + 1) + ".xml";
		InputStream inStream = inClass.getClassLoader().getResourceAsStream(getPackagePath(pkgName) + "/" + fileName);

		if (inStream == null) {
			throw new Exception("Unable to find config file " + fileName + " for class " + className);
		}

		return new InputSource(inStream);
	}

	private static String getPackagePath(String packageName) {
		return packageName.replace('.', '/');
	}

	private String createStatement(StoredProcedure sp) {
		StringBuffer buf = new StringBuffer();

		buf.append("{CALL " + sp.getName() + "(");

		int numArgs = sp.getArguments().size();
		int i;

		for (i = 0; i < numArgs - 1; i++) {
			buf.append("?,");
		}

		if (numArgs > 0) {
			buf.append('?');
		}

		buf.append(")}");

		return buf.toString();
	}

	protected CallableStatement initBatchStatement(StoredProcedure sp, String statementStr) throws SQLException {
		CallableStatement statement = null;
		Connection connection = null;

		connection = openConnection();
		statement = connection.prepareCall(statementStr);

		return statement;
	}

	protected Connection openConnection() {
		ConnectionManager connMgr = ConnectionManager.getInstance();
		return connMgr.openConnection(getDataSourceName());
	}

	private String getDataSourceName() {
		return DataSource.getInstance().getDataSource();
	}

	protected CallableStatement initializeStatement(Connection con, StoredProcedure sp, String statementStr,
			Vector arguments) throws SQLException {
		CallableStatement statement = null;
		Connection connection = con;

		try {
			if (connection == null) {
				connection = openConnection();
			}
			statement = connection.prepareCall(statementStr);

			setStatementInputParameters(statement, sp.getArguments(), arguments);
		} catch (SQLException e) {
			if (statement != null)
				statement.close();
			if (con == null && connection != null)
				connection.close();

			throw e;
		}

		return statement;
	}

	protected void setStatementInputParameters(PreparedStatement statement, Vector argumentDefs, Vector argumentVals)
			throws SQLException {
		for (int i = 0; i < argumentVals.size(); i++) {
			Object val = argumentVals.get(i);
			Attribute attr = (Attribute) argumentDefs.get(i);
			int sqlType = javaClass2SQLType(attr.getTypeName());

			try {
				if (val == null) {
					// set null by type defined in argument list
					statement.setNull(i + 1, sqlType);
				} else {
					if (sqlType == Types.BLOB) {
						// hack because crappy JDBC won't let us create and store a BLOB
						try {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ObjectOutputStream os = new ObjectOutputStream(baos);

							os.writeObject(val);
							statement.setBytes(i + 1, baos.toByteArray());
						} catch (IOException e) {
							throw new SQLException("Error initializing BLOB data" + e.toString());
						}
					} else {
						// if val is a string escape single quotes
						if (val instanceof String) {
							((String) val).replaceAll("'", "''");
						} else if (val instanceof Boolean) {
							if (((Boolean) val).booleanValue()) {
								val = BOOLEAN_CHAR_REP_TRUE;
							} else {
								val = BOOLEAN_CHAR_REP_FALSE;
							}
						}
						statement.setObject(i + 1, val);
					}
				}
			} catch (SQLException se) {
				String err = "Error initializing SQL stored procedure call ";

				err += "Last arg index = " + (i + 1) + ", Attribute name = " + attr.getName();

				throw new SQLException(err + se.toString());
			}
		}
	}

	public static int javaClass2SQLType(String className) {
		initHash();
		return class2SQLTypeMap[Math.abs(className.hashCode()) % TYPE_MAP_SIZE];
	}

	private static void initHash() {
		// initialize with default values
		for (int i = 0; i < TYPE_MAP_SIZE; i++) {
			class2SQLTypeMap[i] = Types.NULL;
		}
		class2SQLTypeMap[Math.abs(new String("java.lang.Integer").hashCode()) % TYPE_MAP_SIZE] = Types.INTEGER;
		class2SQLTypeMap[Math.abs(new String("java.lang.Long").hashCode()) % TYPE_MAP_SIZE] = Types.BIGINT;
		class2SQLTypeMap[Math.abs(new String("java.lang.Boolean").hashCode()) % TYPE_MAP_SIZE] = Types.CHAR;
		class2SQLTypeMap[Math.abs(new String("java.lang.String").hashCode()) % TYPE_MAP_SIZE] = Types.VARCHAR;
		class2SQLTypeMap[Math.abs(new String("java.sql.Timestamp").hashCode()) % TYPE_MAP_SIZE] = Types.TIMESTAMP;
		class2SQLTypeMap[Math.abs(new String("java.sql.Clob").hashCode()) % TYPE_MAP_SIZE] = Types.CLOB;
		class2SQLTypeMap[Math.abs(new String("java.sql.Blob").hashCode()) % TYPE_MAP_SIZE] = Types.BLOB;
		class2SQLTypeMap[Math.abs(new String("java.lang.Double").hashCode()) % TYPE_MAP_SIZE] = Types.DOUBLE;
		class2SQLTypeMap[Math.abs(new String("java.lang.Float").hashCode()) % TYPE_MAP_SIZE] = Types.FLOAT;
		class2SQLTypeMap[Math.abs(new String("java.lang.Short").hashCode()) % TYPE_MAP_SIZE] = Types.SMALLINT;
		class2SQLTypeMap[Math.abs(new String("java.math.BigDecimal").hashCode()) % TYPE_MAP_SIZE] = Types.DECIMAL;
	}

	private static void displayProcedureTiming(String spName, long duration) {
		String msg = spName + "[" + DataSource.getInstance().getDataSource() + "]: " + duration + " ms";

		// check property to see if we should output to system out
		String printProcTimings = CFactory.instance().getProperty("persistence.logging.proctimings.tosystemout");
		if (printProcTimings != null && printProcTimings.equals("Y")) {
			System.out.println(msg);
		}
	}

	public void closeStatement(boolean closeConn, Statement statement) {
		// this should never fail and if it does, we're in trouble
		try {
			if (statement != null) {
				Connection con = statement.getConnection();
				statement.close();
				if (closeConn) {
					closeConnection(con);
				}
				statement = null;
			}
		} catch (SQLException se) {
			System.out.println("**** " + se.getMessage());
		}
	}

	public boolean closeConnection(Connection connection) {
		ConnectionManager connMgr = ConnectionManager.getInstance();
		return connMgr.closeConnection(connection);
	}

	protected void setObjectValue(ValueObject object, String columnName, Object value) throws Exception {
		ValueObjectConfiguration config = object.getConfiguration();
		Attribute attr = config.getAttributeByColumn(columnName);

		// if I don't have an attribute that matches this column name
		// then don't try to set it
		if (attr == null) {
			String err = "SQLPersistence.setObjectValue- no value found for column name " + columnName;

			err += " Attempting to set for object of type " + object.getClass().getName();

			throw new Exception(err);
		}

		setObjectValue(object, attr, value);
	}

	protected void setObjectValue(Object object, Attribute attr, Object value) throws Exception {
		try {
			PropertyDescriptor pd = attr.getPropertyDescriptor();

			if (pd == null) {
				throw new Exception("Unable to get PropertyDescriptor for attribute " + attr.getName() + " of object "
						+ object.getClass().getName());
			}

			Method method = pd.getWriteMethod();

			if (attr.needsConversion()) {
				value = convertReadObject(value, attr);
			}

			Object[] args = { value };

			method.invoke(object, args);
		} catch (InvocationTargetException ite) {
			String err = "Cannot set value " + attr.getName() + " on object " + object.getClass().getName()
					+ " Reason = ";

			throw new Exception(err + ite.getTargetException().toString(), ite);
		} catch (IllegalAccessException iae) {
			String err = "Cannot set value " + attr.getName() + " on object " + object.getClass().getName()
					+ " Reason = ";

			throw new Exception(err + iae.toString(), iae);
		} catch (IllegalArgumentException iarge) {
			String err = "Cannot set value " + attr.getName() + " on object " + object.getClass().getName();

			if (value != null) {
				err += " Value type = " + value.getClass().getName() + " Reason = ";
			} else {
				err += " Value is null. Reason = ";
			}

			System.out.println("SQLPersistence.setObjectValue(): Error - IllegalArgumentException - " + err);

			throw new Exception(err + iarge.toString(), iarge);
		}
	}

	protected Object convertReadObject(Object value, Attribute attr) throws Exception {
		// sanity check...sometimes in config, values may be null ie. blobs especially!!
		// GAM 9/13/02
		if (value == null) {
			return null;
		}

		// convert CLOBs
		try {
			if (value instanceof Clob) {
				if (((Clob) value).length() > 0) {
					return ((Clob) value).getSubString((long) 1, (int) ((Clob) value).length());
				} else {
					return new String();
				}
			}
		} catch (SQLException se) {
			throw new Exception(se.toString());
		}

		// convert Boolean flags
		if ((value instanceof String) && attr.getTypeName().equals("java.lang.Boolean")) {
			if (value.equals(BOOLEAN_CHAR_REP_TRUE) || value.equals("Y")) {
				return Boolean.TRUE;
			}

			return Boolean.FALSE;
		}

		if ((value instanceof java.math.BigDecimal) && attr.getTypeName().equals("java.lang.Double")) {
			return new Double(((java.math.BigDecimal) value).doubleValue());
		}

		// convert Blob
		if (javaClass2SQLType(attr.getTypeName()) == Types.BLOB) {
			try {
				InputStream is = ((Blob) value).getBinaryStream();
				ObjectInputStream ois = new ObjectInputStream(is);

				return ois.readObject();
			} catch (Exception e) {
				throw new Exception("Can't read Blob: " + e.toString());
			}
		}

		return value;

	}

	protected List<ValueObject> readResultSet(ResultSet rs, ValueObjectConfiguration config, Map objectsByPkMap)
			throws Exception {
		// log.debug("Entering SQLPersistence.readResultSet");

		List<ValueObject> results = new ArrayList<ValueObject>();
		ValueObject newOb = null;

		// define these out here, useful debugging when exception thrown
		String colName = "";
		int i = 0;
		String[] colNames = null;
		Object value = null;

		try {
			ResultSetMetaData rsmd = rs.getMetaData();
			int colCount = rsmd.getColumnCount();
			colNames = new String[colCount + 1]; // over allocate by one to account for the 1-based index in resultset
			Attribute[] attributes = new Attribute[colCount + 1];

			for (i = 1; i <= colCount; i++) {
				colNames[i] = rsmd.getColumnName(i);
				attributes[i] = config.getAttributeByColumn(colNames[i]);

				// if I don't have an attribute that matches this column name
				// then don't try to set it
				if (attributes[i] == null) {
					String err = "SQLPersistence.readResultSet - no value found for column name " + colNames[i];

					throw new Exception(err);
				}
			}

			ValueObjectFactory vof = ValueObjectFactory.getInstance();
			int err_count = 0;

			while (rs.next()) {
				newOb = vof.createObjectFromConfig(config);

				try {
					// column numbers are 1-based
					for (i = 1; i <= colCount; i++) {
						// value = rs.getObject(i);
						value = getObjectFromResultSetSmartMemory(rs, i);
						setObjectValue(newOb, attributes[i], value);
					}

					// object is now in sync with database
					if (newOb != null) {
						newOb.setIsModified(false);
					}

					results.add(newOb);

					if (objectsByPkMap != null)
						objectsByPkMap.put(newOb.getId(), newOb);
				} catch (SQLException se) {
					err_count++;

					if (colNames != null && i >= 1 && i < colNames.length)
						colName = colNames[i];

					String err = "Error loading object type " + config.getType().getName();

					if (value != null)
						err += "Column index = " + i + ", Column name = " + colName + ", Last loaded value = " + value
								+ ", " + se.toString();
					else
						err += "Column index = " + i + ", Column name = " + colName + ", " + se.toString();

					if (err_count > 25)
						throw new Exception("Error reading result set: " + err);
				}
			}
		} catch (SQLException se) {
			if (colNames != null && i >= 1 && i < colNames.length)
				colName = colNames[i];

			String err = "Error loading object type " + config.getType().getName();

			if (value != null)
				err += "Column index = " + i + ", Column name = " + colName + ", Value = " + value + ", "
						+ se.toString();
			else
				err += "Column index = " + i + ", Column name = " + colName + ", " + se.toString();

			throw new Exception("Error reading result set: " + err);
		} catch (Exception oce) {
			throw new Exception("Error reading result set: " + oce.toString());
		}

		return results;
	}

	protected Object getObjectFromResultSetSmartMemory(ResultSet rs, int index) throws SQLException {
		Object value = rs.getObject(index);
		if (value != null && value instanceof Integer) {
			intsAllocated++;
			int val = ((Integer) value).intValue();
			if (val >= 0 && val <= 10000) {
				intsAllocatedZeroTo10K++;
			} else if (val > 10000 && val <= 20000) {
				intsAllocated10KTo20K++;
			} else if (val > 20000 && val <= 100000) {
				intsAllocated20KTo100K++;
			} else if (val > 100000) {
				intsAllocatedOver100K++;
			}
			if (val >= -128 && val <= AUTO_BOX_CACHE_MAX) {
				intsAllocatedCached++;
				return Integer.valueOf(val);
			}
		}
		return value;
	}

	private static void displaySQLWarnings(Statement statement) throws SQLException {
		SQLWarning sqlwarn = statement.getWarnings();
		while (sqlwarn != null) {
			StringBuffer s = new StringBuffer();
			if (sqlwarn.getSQLState() != null && sqlwarn.getSQLState().equalsIgnoreCase("0100E")
					&& sqlwarn.getErrorCode() == 464) {
				s.append("TYPE 4 driver issue - ");
			}
			s.append("Message: " + sqlwarn.getMessage());

			// SELECT INTO, UPDATE or FETCH found no rows - Suppressing the log
			if (sqlwarn.getSQLState() != null && sqlwarn.getSQLState().equalsIgnoreCase("02000"))
				break;

			sqlwarn = sqlwarn.getNextWarning();
		}
	}
}
