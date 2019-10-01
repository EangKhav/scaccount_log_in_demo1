package com.ekv.dev.scaccountlogindemo1.presenter;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.ekv.dev.scaccountlogindemo1.MainActivity;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivityPresenter extends AppCompatActivity {
    private String TAG = MainActivityPresenter.class.getName();
    private static final Object mSync = new Object();
    private static WeakReference<byte[]> mReadBuffer;
    void build(){
        String mArchiveSourcePath = new File("bb_jar1").getPath();

        WeakReference<byte[]> readBufferRef;
        byte[] readBuffer = null;
        synchronized (mSync) {
            readBufferRef = mReadBuffer;
            if (readBufferRef != null) {
                mReadBuffer = null;
                readBuffer = readBufferRef.get();
            }
            if (readBuffer == null) {
                readBuffer = new byte[8192];
                readBufferRef = new WeakReference<byte[]>(readBuffer);
            }
        }

        try {
            JarFile jarFile = new JarFile(mArchiveSourcePath);
            java.security.cert.Certificate[] certs = null;

            Enumeration entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry je = (JarEntry) entries.nextElement();
                if (je.isDirectory()) {
                    continue;
                }
                if (je.getName().startsWith("META-INF/")) {
                    continue;
                }
                java.security.cert.Certificate[] localCerts = loadCertificates(jarFile, je, readBuffer);
                if (false) {
                    System.out.println("File " + mArchiveSourcePath + " entry " + je.getName()
                            + ": certs=" + certs + " ("
                            + (certs != null ? certs.length : 0) + ")");
                }
                if (localCerts == null) {
                    System.err.println("Package has no certificates at entry "
                            + je.getName() + "; ignoring!");
                    jarFile.close();
                    return;
                } else if (certs == null) {
                    certs = localCerts;
                } else {
                    // Ensure all certificates match.
                    for (int i = 0; i < certs.length; i++) {
                        boolean found = false;
                        for (int j = 0; j < localCerts.length; j++) {
                            if (certs[i] != null
                                    && certs[i].equals(localCerts[j])) {
                                found = true;
                                break;
                            }
                        }
                        if (!found || certs.length != localCerts.length) {
                            System.err.println("Package has mismatched certificates at entry "
                                    + je.getName() + "; ignoring!");
                            jarFile.close();
                            return; // false
                        }
                    }
                }
            }

            jarFile.close();

            synchronized (mSync) {
                mReadBuffer = readBufferRef;
            }

            if (certs != null && certs.length > 0) {
                final int N = certs.length;

                for (int i = 0; i < N; i++) {
                    String charSig = new String(toChars(certs[i].getEncoded()));
                    Log.d("EKVBB", "Cert#: " + i + "  Type:" + certs[i].getType()
                            + "\nPublic key: " + certs[i].getPublicKey()
                            + "\nHash code: " + certs[i].hashCode()
                            + " / 0x" + Integer.toHexString(certs[i].hashCode())
                            + "\nTo char: " + charSig);
                }
            } else {
                System.err.println("Package has no certificates; ignoring!");
                return;
            }
        } catch (CertificateEncodingException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            System.err.println("Exception reading " + mArchiveSourcePath + "\n" + e);
            return;
        } catch (RuntimeException e) {
            System.err.println("Exception reading " + mArchiveSourcePath + "\n" + e);
            return;
        }
    }
    public void WriteSignature(String packageName) {
        // all of this is fairly well documented
        // if it doesn't work, just search around.

        PackageManager pm = this.getPackageManager();
        PackageInfo pi = null;
        try {
            pi = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e1) {
            e1.printStackTrace();
        }
        Signature[] s = pi.signatures;

        // you can use toChars or get the hashcode, whatever
        String sig = new String(s[0].toChars());

        try {
            File root = Environment.getExternalStorageDirectory();
            if ( root.canWrite() )
            {
                // toChars is long, so i write it to a file on the external storage
                File f = new File(root, "signature.txt");
                FileWriter fw = new FileWriter(f);
                BufferedWriter out = new BufferedWriter(fw);
                out.write(packageName + "\nSignature: " + sig);
                out.close();
                fw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void printHashKey(){
            try {
                PackageManager packageManager = getPackageManager();
                List<PackageInfo> packageInfos;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                     packageInfos = packageManager.getInstalledPackages(PackageManager.GET_SIGNING_CERTIFICATES);
                }else {
                    packageInfos = packageManager.getInstalledPackages(PackageManager.GET_SIGNATURES);
                }

                for (PackageInfo packageInfo : packageInfos){
                    String packageName = packageInfo.applicationInfo.loadLabel(packageManager).toString();
                    String vendorName = packageInfo.packageName;
                    Log.d(TAG, "printHashKey: Package Name -> "+packageName);
                    Log.d(TAG, "printHashKey: Vendor Name -> "+vendorName);
                    Signature[] signatures;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                         signatures = packageInfo.signingInfo.getApkContentsSigners();
                    }else {
                        signatures = packageInfo.signatures;
                    }
                    for (Signature signature : signatures){
                        byte[] rawCert = signature.toByteArray();
                        InputStream inputStream = new ByteArrayInputStream(rawCert);
                        try{
                            CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
                            X509Certificate x509Certificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);
                            Log.d(TAG, "printHashKey: Certificate subject -> "+x509Certificate.getSubjectDN());
                            Log.d(TAG, "printHashKey: Certificate Issuer -> "+x509Certificate.getIssuerDN());
                            Log.d(TAG, "printHashKey: Certificate Serial Name -> "+x509Certificate.getSerialNumber());
                            Log.d(TAG, "printHashKey: Certificate Signature ->"+ Arrays.toString(x509Certificate.getSignature()));
                        }catch (CertificateException e){
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
    }
    public java.security.cert.Certificate[] loadCertificates(JarFile jarFile, JarEntry je, byte[] readBuffer) {
        try {
            // We must read the stream for the JarEntry to retrieve
            // its certificates.
            InputStream is = jarFile.getInputStream(je);
            while (is.read(readBuffer, 0, readBuffer.length) != -1) {
                // not using
            }
            is.close();

            return je != null ? je.getCertificates() : null;
        } catch (IOException e) {
            System.err.println("Exception reading " + je.getName() + " in "
                    + jarFile.getName() + ": " + e);
        }
        return null;
    }
    public char[] toChars(byte[] mSignature) {
        byte[] sig = mSignature;
        final int N = sig.length;
        final int N2 = N*2;
        char[] text = new char[N2];

        for (int j=0; j<N; j++) {
            byte v = sig[j];
            int d = (v>>4)&0xf;
            text[j*2] = (char)(d >= 10 ? ('a' + d - 10) : ('0' + d));
            d = v&0xf;
            text[j*2+1] = (char)(d >= 10 ? ('a' + d - 10) : ('0' + d));
        }
        return text;
    }

    public  void printMyHashKey() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String hashKey = new String(Base64.encode(md.digest(), 0));
                Log.i(TAG, "printHashKey() Hash Key: " + hashKey);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "printHashKey()", e);
        } catch (Exception e) {
            Log.e(TAG, "printHashKey()", e);
        }
    }
}
