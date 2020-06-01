/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.certificate.management;

import com.sun.enterprise.universal.xml.MiniXmlParser;
import com.sun.enterprise.universal.xml.MiniXmlParserException;

import java.io.File;
import java.util.Map;

import static com.sun.enterprise.util.StringUtils.ok;

/**
 * Helper methods for various Certificate Management commands.
 *
 * @author Andrew Pielage
 */
public class CertificateManagementUtils {

    public static final String DEFAULT_KEYSTORE = "${com.sun.aas.instanceRoot}"
            + File.separator + "config" + File.separator + "keystore.jks";
    public static final String DEFAULT_TRUSTSTORE = "${com.sun.aas.instanceRoot}"
            + File.separator + "config" + File.separator + "cacerts.jks";

    /**
     * Determines and returns the key store.
     *
     * @param parser      The {@link MiniXmlParser} for extracting info from the domain.xml
     * @param listener    The name of the HTTP or IIOP listener to get the key store from. Can be null.
     * @param instanceDir The directory of the target instance, used for relative paths
     * @return The key store of the target
     * @throws MiniXmlParserException If there's an issue reading the domain.xml
     */
    public static File resolveKeyStore(MiniXmlParser parser, String listener, File instanceDir)
            throws MiniXmlParserException {
        File keystore = null;
        if (listener != null) {
            // Check if listener is an HTTP listener
            keystore = getStoreFromHttpListeners(parser, listener, "key-store", instanceDir);

            if (keystore == null) {
                // Check if listener is an IIOP listener
                keystore = getStoreFromIiopListeners(parser, listener, "key-store", instanceDir);
            }
        }

        // Default to getting it from the JVM options if no non-default value found
        if (keystore == null) {
            keystore = getStoreFromJvmOptions(parser, "keyStore", instanceDir);
        }

        // If it's STILL null, just go with default
        if (keystore == null) {
            keystore = new File(DEFAULT_KEYSTORE);
        }

        return keystore;
    }

    /**
     * Determines and returns the trust store.
     *
     * @param parser      The {@link MiniXmlParser} for extracting info from the domain.xml
     * @param listener    The name of the HTTP or IIOP listener to get the trust store from. Can be null.
     * @param instanceDir The directory of the target instance, used for relative paths
     * @return The trust store of the target
     * @throws MiniXmlParserException If there's an issue reading the domain.xml
     */
    public static File resolveTrustStore(MiniXmlParser parser, String listener, File instanceDir)
            throws MiniXmlParserException {
        File truststore = null;
        if (listener != null) {
            // Check if listener is an HTTP listener
            truststore = getStoreFromHttpListeners(parser, listener, "trust-store", instanceDir);

            if (truststore == null) {
                // Check if listener is an IIOP listener
                truststore = getStoreFromIiopListeners(parser, listener, "trust-store", instanceDir);
            }
        }

        // Default to getting it from the JVM options if no non-default value found
        if (truststore == null) {
            truststore = getStoreFromJvmOptions(parser, "trustStore", instanceDir);
        }

        // If it's STILL null, just go with default
        if (truststore == null) {
            truststore = new File(DEFAULT_TRUSTSTORE);
        }

        return truststore;
    }

    /**
     * Gets the store from a target HTTP listener
     *
     * @param parser         The {@link MiniXmlParser} for extracting info from the domain.xml
     * @param listener       The name of the HTTP listener to get the store from.
     * @param storeAttribute The name of the store attribute to get (should be "key-store" or "trust-store")
     * @param instanceDir    The directory of the target instance, used for relative paths
     * @return The store of the target, or null if no matching listener or no store configured
     * @throws MiniXmlParserException If there's an issue reading the domain.xml
     */
    private static File getStoreFromHttpListeners(MiniXmlParser parser, String listener,
            String storeAttribute, File instanceDir) throws MiniXmlParserException {
        for (Map<String, String> listenerAttributes : parser.getProtocolAttributes()) {
            if (listenerAttributes.get("name").equals(listener)) {
                // Get the keystore from the listener if it has a custom one
                return getStoreFromListenerAttribute(listenerAttributes.get(storeAttribute), instanceDir);
            }
        }
        return null;
    }

    /**
     * Gets the store from a target IIOP listener
     *
     * @param parser         The {@link MiniXmlParser} for extracting info from the domain.xml
     * @param listener       The name of the IIOP listener to get the store from.
     * @param storeAttribute The name of the store attribute to get (should be "key-store" or "trust-store")
     * @param instanceDir    The directory of the target instance, used for relative paths
     * @return The store of the target, or null if no matching listener or no store configured
     * @throws MiniXmlParserException If there's an issue reading the domain.xml
     */
    private static File getStoreFromIiopListeners(MiniXmlParser parser, String listener,
            String storeAttribute, File instanceDir) throws MiniXmlParserException {
        for (Map<String, String> listenerAttributes : parser.getIiopSslAttributes()) {
            if (listenerAttributes.get("id").equals(listener)) {
                // Get the keystore from the listener if it has a custom one
                return getStoreFromListenerAttribute(listenerAttributes.get(storeAttribute), instanceDir);
            }
        }
        return null;
    }

    /**
     * Helper method that returns the store from a target listener's config attribute,
     * making any relative paths absolute.
     *
     * @param storePath   The path to the store to return as a {@link File}
     * @param instanceDir The instance directory, used for making relative paths absolute
     * @return The absolute path of the target store, or null if no store given
     */
    private static File getStoreFromListenerAttribute(String storePath, File instanceDir) {
        if (!ok(storePath)) {
            return null;
        }

        File store = new File(storePath);
        if (!store.isAbsolute()) {
            store = new File(instanceDir.getAbsolutePath() + File.separator + store.getPath());
        }

        return store;
    }

    /**
     * Gets the store from a target config's JVM options
     *
     * @param parser      The {@link MiniXmlParser} for extracting info from the domain.xml
     * @param storeName   The JVM option name of the store (should be keyStore or trustStore)
     * @param instanceDir The instance directory, used for SystemProperty substitution
     * @return The absolute path of the target store
     * @throws MiniXmlParserException If there's an issue reading the domain.xml
     */
    private static File getStoreFromJvmOptions(MiniXmlParser parser, String storeName, File instanceDir)
            throws MiniXmlParserException {
        for (MiniXmlParser.JvmOption jvmOption : parser.getJvmOptions()) {
            if (jvmOption.toString().startsWith("-Djavax.net.ssl." + storeName + "=")) {
                return new File(jvmOption.toString().split("=")[1]
                        .replace("${com.sun.aas.instanceRoot}", instanceDir.getAbsolutePath()));
            }
        }
        return null;
    }

    /**
     * Constructs the command to pass to keytool for creating a self-signed cert
     *
     * @param keystore The key store to add the certificate to
     * @param password The password for the key store
     * @param alias    The alias of the certificate
     * @param dname    The distinguished name of the certificate
     * @param altnames The alternative names of the certificate
     * @return A String array to pass to {@link com.sun.enterprise.admin.servermgmt.KeystoreManager.KeytoolExecutor}
     */
    public static String[] constructGenerateCertKeytoolCommand(File keystore, char[] password,
            String alias, String dname, String[] altnames) {
        String[] keytoolCmd = new String[]{"-genkeypair", "-keyalg", "RSA", "-keystore", keystore.getAbsolutePath(),
                "-alias", alias, "-dname", dname,
                "-validity", "365", "-keypass", new String(password), "-storepass", new String(password)};

        if (altnames != null && altnames.length != 0) {
            keytoolCmd = addSubjectAlternativeNames(keytoolCmd, altnames);
        }

        return keytoolCmd;
    }

    /**
     * Helper method that formats the String array of alternative names into the format that the keytool expects.
     *
     * @param keytoolCmd       The String array containing the keytool command before alternative names have been added.
     * @param alternativeNames The String array containing the alternatives names
     * @return A String array of the original keytool command with the alternative names added
     */
    protected static String[] addSubjectAlternativeNames(String[] keytoolCmd, String[] alternativeNames) {
        // Create a new array to make room for the extra commands
        String[] expandedKeytoolCmd = new String[keytoolCmd.length + 2];
        System.arraycopy(keytoolCmd, 0, expandedKeytoolCmd, 0, keytoolCmd.length);

        int i = keytoolCmd.length;
        expandedKeytoolCmd[i] = "-ext";
        expandedKeytoolCmd[i + 1] = "SAN=";

        for (String altName : alternativeNames) {
            // Check if the altname was provided without any additional info, assuming it's DNS if so
            if (!altName.contains(",") && !altName.contains(":")) {
                expandedKeytoolCmd[i + 1] += "DNS:" + altName;
            } else {
                expandedKeytoolCmd[i + 1] += altName;
            }
            expandedKeytoolCmd[i + 1] += ",";
        }

        // Remove trailing comma
        expandedKeytoolCmd[i + 1] = expandedKeytoolCmd[i + 1].substring(0, expandedKeytoolCmd[i + 1].length() - 1);

        return expandedKeytoolCmd;
    }

    /**
     * Constructs the command to pass to keytool for adding the self-signed cert to the trust store
     *
     * @param keystore           The target key store that the certificate was added to
     * @param truststore         The target trust store to add the certificate to
     * @param keystorePassword   The password for the key store
     * @param truststorePassword The password for the trust store
     * @param alias              The alias of the certificate
     * @return A String array to pass to {@link com.sun.enterprise.admin.servermgmt.KeystoreManager.KeytoolExecutor}
     */
    public static String[] constructImportCertKeytoolCommand(File keystore, File truststore, char[] keystorePassword,
            char[] truststorePassword, String alias) {
        String[] keytoolCmd = new String[]{"-importkeystore", "-srckeystore", keystore.getAbsolutePath(),
                "-destkeystore", truststore.getAbsolutePath(), "-srcalias", alias, "-destalias", alias,
                "-srcstorepass", new String(keystorePassword), "-deststorepass", new String(truststorePassword),
                "-srckeypass", new String(keystorePassword), "-destkeypass", new String(truststorePassword),
                "-noprompt"};

        return keytoolCmd;
    }

    /**
     * @param parser    The {@link MiniXmlParser} for extracting info from the domain.xml
     * @param listener  The name of the listener to get the password from.
     * @param attribute The name of the store password attribute (should be key-store-password or trust-store-password)
     * @return A char array containing the password of the target listener, or null if no matches or password found
     * @throws MiniXmlParserException if there's an issue reading the domain.xml
     */
    public static char[] getPasswordFromListener(MiniXmlParser parser, String listener, String attribute)
            throws MiniXmlParserException {
        char[] password = null;
        for (Map<String, String> listenerAttributes : parser.getProtocolAttributes()) {
            if (listenerAttributes.get("name").equals(listener)) {
                // Get the keystore from the listener if it has a custom one
                password = listenerAttributes.get(attribute).toCharArray();
            }
        }

        if (password == null || password.length == 0) {
            for (Map<String, String> listenerAttributes : parser.getIiopSslAttributes()) {
                if (listenerAttributes.get("id").equals(listener)) {
                    // Get the keystore from the listener if it has a custom one
                    password = listenerAttributes.get(attribute).toCharArray();
                }
            }
        }

        return password;
    }
}
