/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.runtime;

import groovy.lang.*;
import groovy.xml.dom.DOMUtil;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.codehaus.groovy.runtime.typehandling.IntegerCache;
import org.w3c.dom.Element;

import java.beans.Introspector;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A static helper class to make bytecode generation easier and act as a facade over the Invoker
 *
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class InvokerHelper {
    public static final Object[] EMPTY_ARGS = {
    };

    private static final Object[] EMPTY_MAIN_ARGS = new Object[]{new String[0]};

    private static final Invoker SINGLETON = new Invoker();


    public static MetaClass getMetaClass(Object object) {
        return getInstance().getMetaClass(object);
    }

    public static void removeClass(Class clazz) {
        getInstance().removeMetaClass(clazz);
        Introspector.flushFromCaches(clazz);
    }

    public static Invoker getInstance() {
        return SINGLETON;
    }

    public static Object invokeNoArgumentsMethod(Object object, String methodName) {
        return getInstance().invokeMethod(object, methodName, EMPTY_ARGS);
    }

    public static Object invokeMethod(Object object, String methodName, Object arguments) {
        return getInstance().invokeMethod(object, methodName, arguments);
    }

    public static Object invokeSuperMethod(Object object, String methodName, Object arguments) {
        return getInstance().invokeSuperMethod(object, methodName, arguments);
    }

    public static Object invokeMethodSafe(Object object, String methodName, Object arguments) {
        if (object != null) {
            return getInstance().invokeMethod(object, methodName, arguments);
        }
        return null;
    }

    public static Object invokeStaticMethod(Class type, String methodName, Object arguments) {
        return getInstance().invokeStaticMethod(type, methodName, arguments);
    }

    public static Object invokeStaticMethod(String klass, String methodName, Object arguments) throws ClassNotFoundException {
        Class type = Class.forName(klass);
        return getInstance().invokeStaticMethod(type, methodName, arguments);
    }


    public static Object invokeStaticNoArgumentsMethod(Class type, String methodName) {
        return getInstance().invokeStaticMethod(type, methodName, EMPTY_ARGS);
    }

    public static Object invokeConstructorOf(Class type, Object arguments) {
        return getInstance().invokeConstructorOf(type, arguments);
    }

    public static Object invokeConstructorOf(String klass, Object arguments) throws ClassNotFoundException {
        Class type = Class.forName(klass);
        return getInstance().invokeConstructorOf(type, arguments);
    }

    public static Object invokeNoArgumentsConstructorOf(Class type) {
        return getInstance().invokeConstructorOf(type, EMPTY_ARGS);
    }

    public static Object invokeClosure(Object closure, Object arguments) {
        return getInstance().invokeMethod(closure, "doCall", arguments);
    }

    public static List asList(Object value) {
        if (value == null) {
            return Collections.EMPTY_LIST;
        }
        if (value instanceof List) {
            return (List) value;
        }
        if (value.getClass().isArray()) {
            return Arrays.asList((Object[]) value);
        }
        if (value instanceof Enumeration) {
            List answer = new ArrayList();
            for (Enumeration e = (Enumeration) value; e.hasMoreElements();) {
                answer.add(e.nextElement());
            }
            return answer;
        }
        // lets assume its a collection of 1
        return Collections.singletonList(value);
    }

    public static String toString(Object arguments) {
        if (arguments instanceof Object[])
            return toArrayString((Object[]) arguments);
        if (arguments instanceof Collection)
            return toListString((Collection) arguments);
        if (arguments instanceof Map)
            return toMapString((Map) arguments);
        if (arguments instanceof Collection)
            return format(arguments, true);
        return format(arguments, false);
    }

    public static String inspect(Object self) {
        return format(self, true);
    }

    public static Object getAttribute(Object object, String attribute) {
        return getInstance().getAttribute(object, attribute);
    }

    public static void setAttribute(Object object, String attribute, Object newValue) {
        getInstance().setAttribute(object, attribute, newValue);
    }

    public static Object getProperty(Object object, String property) {
        return getInstance().getProperty(object, property);
    }

    public static Object getPropertySafe(Object object, String property) {
        if (object != null) {
            return getInstance().getProperty(object, property);
        }
        return null;
    }

    public static void setProperty(Object object, String property, Object newValue) {
        getInstance().setProperty(object, property, newValue);
    }

    /**
     * This is so we don't have to reorder the stack when we call this method.
     * At some point a better name might be in order.
     */
    public static void setProperty2(Object newValue, Object object, String property) {
        getInstance().setProperty(object, property, newValue);
    }


    /**
     * This is so we don't have to reorder the stack when we call this method.
     * At some point a better name might be in order.
     */
    public static void setGroovyObjectProperty(Object newValue, GroovyObject object, String property) {
        object.setProperty(property, newValue);
    }

    public static Object getGroovyObjectProperty(GroovyObject object, String property) {
        return object.getProperty(property);
    }


    /**
     * This is so we don't have to reorder the stack when we call this method.
     * At some point a better name might be in order.
     */
    public static void setPropertySafe2(Object newValue, Object object, String property) {
        if (object != null) {
            setProperty2(newValue, object, property);
        }
    }

    /**
     * Returns the method pointer for the given object name
     */
    public static Closure getMethodPointer(Object object, String methodName) {
        return getInstance().getMethodPointer(object, methodName);
    }

    public static Object unaryMinus(Object value) {
        if (value instanceof Integer) {
            Integer number = (Integer) value;
            return IntegerCache.integerValue(-number.intValue());
        }
        if (value instanceof Long) {
            Long number = (Long) value;
            return new Long(-number.longValue());
        }
        if (value instanceof BigInteger) {
            return ((BigInteger) value).negate();
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).negate();
        }
        if (value instanceof Double) {
            Double number = (Double) value;
            return new Double(-number.doubleValue());
        }
        if (value instanceof Float) {
            Float number = (Float) value;
            return new Float(-number.floatValue());
        }
        if (value instanceof ArrayList) {
            // value is an list.
            List newlist = new ArrayList();
            Iterator it = ((ArrayList) value).iterator();
            for (; it.hasNext();) {
                newlist.add(unaryMinus(it.next()));
            }
            return newlist;
        }
        return invokeMethod(value, "negative", new Object[0]);
    }

    public static Object unaryPlus(Object value) {
        if (value instanceof Integer ||
                value instanceof Long ||
                value instanceof BigInteger ||
                value instanceof BigDecimal ||
                value instanceof Double ||
                value instanceof Float) {
            return value;
        }
        if (value instanceof ArrayList) {
            // value is an list.
            List newlist = new ArrayList();
            Iterator it = ((ArrayList) value).iterator();
            for (; it.hasNext();) {
                newlist.add(unaryPlus(it.next()));
            }
            return newlist;
        }
        return invokeMethod(value, "positive", new Object[0]);
    }

    /**
     * Find the right hand regex within the left hand string and return a matcher.
     *
     * @param left  string to compare
     * @param right regular expression to compare the string to
     */
    public static Matcher findRegex(Object left, Object right) {
        String stringToCompare;
        if (left instanceof String) {
            stringToCompare = (String) left;
        } else {
            stringToCompare = toString(left);
        }
        String regexToCompareTo;
        if (right instanceof String) {
            regexToCompareTo = (String) right;
        } else if (right instanceof Pattern) {
            Pattern pattern = (Pattern) right;
            return pattern.matcher(stringToCompare);
        } else {
            regexToCompareTo = toString(right);
        }
        Matcher matcher = Pattern.compile(regexToCompareTo).matcher(stringToCompare);
        return matcher;
    }


    /**
     * Find the right hand regex within the left hand string and return a matcher.
     *
     * @param left  string to compare
     * @param right regular expression to compare the string to
     */
    public static boolean matchRegex(Object left, Object right) {
        Pattern pattern;
        if (right instanceof Pattern) {
            pattern = (Pattern) right;
        } else {
            pattern = Pattern.compile(toString(right));
        }
        String stringToCompare = toString(left);
        Matcher matcher = pattern.matcher(stringToCompare);
        RegexSupport.setLastMatcher(matcher);
        return matcher.matches();
    }

    public static Tuple createTuple(Object[] array) {
        return new Tuple(array);
    }

    public static SpreadMap spreadMap(Object value) {
        if (value instanceof Map) {
            Object[] values = new Object[((Map) value).keySet().size() * 2];
            int index = 0;
            Iterator it = ((Map) value).keySet().iterator();
            for (; it.hasNext();) {
                Object key = it.next();
                values[index++] = key;
                values[index++] = ((Map) value).get(key);
            }
            return new SpreadMap(values);
        }
        throw new SpreadMapEvaluatingException("Cannot spread the map " + value.getClass().getName() + ", value " + value);
    }

    public static List createList(Object[] values) {
        List answer = new ArrayList(values.length);
        answer.addAll(Arrays.asList(values));
        return answer;
    }

    public static Map createMap(Object[] values) {
        Map answer = new LinkedHashMap(values.length / 2);
        int i = 0;
        while (i < values.length - 1) {
            if ((values[i] instanceof SpreadMap) && (values[i + 1] instanceof Map)) {
                Map smap = (Map) values[i + 1];
                Iterator iter = smap.keySet().iterator();
                for (; iter.hasNext();) {
                    Object key = iter.next();
                    answer.put(key, smap.get(key));
                }
                i += 2;
            } else {
                answer.put(values[i++], values[i++]);
            }
        }
        return answer;
    }

    public static void assertFailed(Object expression, Object message) {
        if (message == null || "".equals(message)) {
            throw new AssertionError("Expression: " + expression);
        }
        throw new AssertionError(String.valueOf(message) + ". Expression: " + expression);
    }

    public static Object runScript(Class scriptClass, String[] args) {
        Binding context = new Binding(args);
        Script script = createScript(scriptClass, context);
        return invokeMethod(script, "run", EMPTY_ARGS);
    }

    public static Script createScript(Class scriptClass, Binding context) {
        // for empty scripts
        if (scriptClass == null) {
            return new Script() {
                public Object run() {
                    return null;
                }
            };
        }
        try {
            final GroovyObject object = (GroovyObject) scriptClass.newInstance();
            Script script = null;
            if (object instanceof Script) {
                script = (Script) object;
            } else {
                // it could just be a class, so lets wrap it in a Script wrapper
                // though the bindings will be ignored
                script = new Script() {
                    public Object run() {
                        object.invokeMethod("main", EMPTY_MAIN_ARGS);
                        return null;
                    }
                };
                setProperties(object, context.getVariables());
            }
            script.setBinding(context);
            return script;
        }
        catch (Exception e) {
            throw new GroovyRuntimeException("Failed to create Script instance for class: " + scriptClass + ". Reason: " + e,
                    e);
        }
    }

    /**
     * Sets the properties on the given object
     */
    public static void setProperties(Object object, Map map) {
        MetaClass mc = getInstance().getMetaClass(object);
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = entry.getKey().toString();

            Object value = entry.getValue();
            try {
                mc.setProperty(object, key, value);
            } catch (MissingPropertyException mpe) {
                // Ignore
            }
        }
    }

    public static String getVersion() {
        String version = null;
        Package p = Package.getPackage("groovy.lang");
        if (p != null) {
            version = p.getImplementationVersion();
        }
        if (version == null) {
            version = "";
        }
        return version;
    }

    /**
     * Writes the given object to the given stream
     */
    public static void write(Writer out, Object object) throws IOException {
        if (object instanceof String) {
            out.write((String) object);
        } else if (object instanceof Object[]) {
            out.write(toArrayString((Object[]) object));
        } else if (object instanceof Map) {
            out.write(toMapString((Map) object));
        } else if (object instanceof Collection) {
            out.write(toListString((Collection) object));
        } else if (object instanceof Writable) {
            Writable writable = (Writable) object;
            writable.writeTo(out);
        } else if (object instanceof InputStream || object instanceof Reader) {
            // Copy stream to stream
            Reader reader;
            if (object instanceof InputStream) {
                reader = new InputStreamReader((InputStream) object);
            } else {
                reader = (Reader) object;
            }
            char[] chars = new char[8192];
            int i;
            while ((i = reader.read(chars)) != -1) {
                out.write(chars, 0, i);
            }
            reader.close();
        } else {
            out.write(toString(object));
        }
    }

    public static Iterator asIterator(Object o) {
        return (Iterator) invokeMethod(o, "iterator", EMPTY_ARGS);
    }

    protected static String format(Object arguments, boolean verbose) {
        if (arguments == null) {
            final NullObject nullObject = NullObject.getNullObject();
            return (String) nullObject.getMetaClass().invokeMethod(nullObject, "toString", EMPTY_ARGS);
        }
        if (arguments.getClass().isArray()) {
            return format(DefaultTypeTransformation.asCollection(arguments), verbose);
        }
        if (arguments instanceof Range) {
            Range range = (Range) arguments;
            if (verbose) {
                return range.inspect();
            } else {
                return range.toString();
            }
        }
        if (arguments instanceof List) {
            List list = (List) arguments;
            StringBuffer buffer = new StringBuffer("[");
            boolean first = true;
            for (Iterator iter = list.iterator(); iter.hasNext();) {
                if (first) {
                    first = false;
                } else {
                    buffer.append(", ");
                }
                buffer.append(format(iter.next(), verbose));
            }
            buffer.append("]");
            return buffer.toString();
        }
        if (arguments instanceof Map) {
            Map map = (Map) arguments;
            if (map.isEmpty()) {
                return "[:]";
            }
            StringBuffer buffer = new StringBuffer("[");
            boolean first = true;
            for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
                if (first) {
                    first = false;
                } else {
                    buffer.append(", ");
                }
                Map.Entry entry = (Map.Entry) iter.next();
                buffer.append(format(entry.getKey(), verbose));
                buffer.append(":");
                if (entry.getValue() == map) {
                    buffer.append("this Map_");
                } else {
                    buffer.append(format(entry.getValue(), verbose));
                }
            }
            buffer.append("]");
            return buffer.toString();
        }
        if (arguments instanceof Element) {
            return DOMUtil.serialize((Element) arguments);
        }
        if (arguments instanceof String) {
            if (verbose) {
                String arg = ((String) arguments).replaceAll("\\n", "\\\\n");    // line feed
                arg = arg.replaceAll("\\r", "\\\\r");      // carriage return
                arg = arg.replaceAll("\\t", "\\\\t");      // tab
                arg = arg.replaceAll("\\f", "\\\\f");      // form feed
                arg = arg.replaceAll("\\\"", "\\\\\"");    // double quotation amrk
                arg = arg.replaceAll("\\\\", "\\\\");      // back slash
                return "\"" + arg + "\"";
            } else {
                return (String) arguments;
            }
        }
        return arguments.toString();
    }


    /**
     * A helper method to format the arguments types as a comma-separated list
     */
    public static String toTypeString(Object[] arguments) {
        if (arguments == null) {
            return "null";
        }
        StringBuffer argBuf = new StringBuffer();
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                argBuf.append(", ");
            }
            argBuf.append(arguments[i] != null ? arguments[i].getClass().getName() : "null");
        }
        return argBuf.toString();
    }

    /**
     * A helper method to return the string representation of a map with bracket boundaries "[" and "]".
     */
    public static String toMapString(Map arg) {
        return format(arg, true);
        /*if (arg == null) {
            return "null";
        }
        if (arg.isEmpty()) {
            return "[:]";
        }
        String sbdry = "[";
        String ebdry = "]";
        StringBuffer buffer = new StringBuffer(sbdry);
        boolean first = true;
        for (Iterator iter = arg.entrySet().iterator(); iter.hasNext();) {
            if (first)
                first = false;
            else
                buffer.append(", ");
            Map.Entry entry = (Map.Entry) iter.next();
            buffer.append(format(entry.getKey(), true));
            buffer.append(":");
            buffer.append(format(entry.getValue(), true));
        }
        buffer.append(ebdry);
        return buffer.toString();*/
    }

    /**
     * A helper method to return the string representation of a list with bracket boundaries "[" and "]".
     */
    public static String toListString(Collection arg) {
        if (arg == null) {
            return "null";
        }
        if (arg.isEmpty()) {
            return "[]";
        }
        String sbdry = "[";
        String ebdry = "]";
        StringBuffer buffer = new StringBuffer(sbdry);
        boolean first = true;
        for (Iterator iter = arg.iterator(); iter.hasNext();) {
            if (first)
                first = false;
            else
                buffer.append(", ");
            Object elem = iter.next();
            buffer.append(format(elem, true));
        }
        buffer.append(ebdry);
        return buffer.toString();
    }

    /**
     * A helper method to return the string representation of an arrray of objects
     * with brace boundaries "{" and "}".
     */
    public static String toArrayString(Object[] arguments) {
        if (arguments == null) {
            return "null";
        }
        String sbdry = "{";
        String ebdry = "}";
        StringBuffer argBuf = new StringBuffer(sbdry);
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                argBuf.append(", ");
            }
            argBuf.append(format(arguments[i], true));
        }
        argBuf.append(ebdry);
        return argBuf.toString();
    }

    public static List createRange(Object from, Object to, boolean inclusive) {
        try {
            return ScriptBytecodeAdapter.createRange(from, to, inclusive);
        } catch (RuntimeException re) {
            throw re;
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Object bitwiseNegate(Object value) {
        if (value instanceof Integer) {
            Integer number = (Integer) value;
            return new Integer(~number.intValue());
        }
        if (value instanceof Long) {
            Long number = (Long) value;
            return new Long(~number.longValue());
        }
        if (value instanceof BigInteger) {
            return ((BigInteger) value).not();
        }
        if (value instanceof String) {
            // value is a regular expression.
            return DefaultGroovyMethods.bitwiseNegate(value.toString());
        }
        if (value instanceof GString) {
            // value is a regular expression.
            return DefaultGroovyMethods.bitwiseNegate(value.toString());
        }
        if (value instanceof ArrayList) {
            // value is an list.
            List newlist = new ArrayList();
            Iterator it = ((ArrayList) value).iterator();
            for (; it.hasNext();) {
                newlist.add(bitwiseNegate(it.next()));
            }
            return newlist;
        }
        return invokeMethod(value, "bitwiseNegate", new Object[0]);
    }

}
