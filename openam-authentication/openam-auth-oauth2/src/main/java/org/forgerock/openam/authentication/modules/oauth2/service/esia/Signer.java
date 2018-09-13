package org.forgerock.openam.authentication.modules.oauth2.service.esia;

import java.io.FileReader;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.am.util.SystemProperties;

public class Signer {
	
	final static Logger logger = LoggerFactory.getLogger(Signer.class);

	private static X509CertificateHolder getCert() {
		return Signer.certHolder;
	}

	private static PrivateKey getPrivateKey() {
		return Signer.privateKey;
	}

	@SuppressWarnings("rawtypes")
	public static String signString(String data)  {
		if(privateKey == null || certHolder == null)
			initKeys();
		String encoded = null;
		Security.addProvider(new BouncyCastleProvider());

		List<X509CertificateHolder> certList = new ArrayList<>();
		CMSTypedData msg = new CMSProcessableByteArray(data.getBytes()); 

		certList.add(getCert()); // Adding the X509 Certificate
		try {
			Store certs = new JcaCertStore(certList);
	
			CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
			// Initializing the the BC's Signer
			ContentSigner shaSigner = new JcaContentSignerBuilder("SHA256withRSA")
					.setProvider("BC").build(getPrivateKey());
	
			gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
					new JcaDigestCalculatorProviderBuilder().setProvider("BC")
							.build()).build(shaSigner, getCert()));
	
			gen.addCertificates(certs);
	
			CMSSignedData sigData = gen.generate(msg, false);
			sigData.getSignerInfos();
			
			encoded = Base64.encodeBase64URLSafeString(sigData.getEncoded());
		} catch(Exception e) {
			logger.error("error sign string{} {}", data, e.toString());
		}

		return encoded;

	}

	static X509CertificateHolder certHolder;
	static PrivateKey privateKey;
	
	//TODO get cert paths from config
	static final String keyPath = SystemProperties.get(Signer.class.getName().concat(".keyPath"), "/etc/nginx/ssl/example.key");
	static final String certPath = SystemProperties.get(Signer.class.getName().concat(".certPath"), "/etc/nginx/ssl/example.crt");

	private static synchronized void initKeys() {
		
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		PEMParser pemReader = null;
		PEMParser certPemReader = null;
		try {

			Object obj = null;
			try (FileReader fileReader = new FileReader(keyPath)) {
				pemReader = new PEMParser(fileReader);
				obj = pemReader.readObject();
				// pemObject = pemReader.readPemObject();
				pemReader.close();
			}
			Object certObj = null;
			try (FileReader fileCertReader = new FileReader(certPath)) {
				certPemReader = new PEMParser(fileCertReader);
				certObj = certPemReader.readObject();
				// certPemObject = certPemReader.readPemObject();
				certPemReader.close();
			}

			if(obj instanceof PEMKeyPair)
				privateKey = new JcaPEMKeyConverter().getKeyPair((PEMKeyPair) obj).getPrivate();
			else 
				privateKey = new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) obj );

			certHolder = ((X509CertificateHolder) certObj);

		} catch (IOException ex) {
			logger.error("error init keys {}", ex.toString());
		}
	}
}