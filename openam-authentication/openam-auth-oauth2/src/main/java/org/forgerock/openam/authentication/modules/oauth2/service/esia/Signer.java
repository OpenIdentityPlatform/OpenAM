package org.forgerock.openam.authentication.modules.oauth2.service.esia;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.iplanet.am.util.SystemProperties;
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

import java.io.FileReader;
import java.security.PrivateKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Signer {
	
	final static Logger logger = LoggerFactory.getLogger(Signer.class);
	static {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}

	private static final Cache<String, X509CertificateHolder> certificateHolderCache = CacheBuilder.newBuilder().maximumSize(10)
			.expireAfterWrite(10, TimeUnit.MINUTES).build();

	private static final Cache<String, PrivateKey> privateKeyCache = CacheBuilder.newBuilder().maximumSize(10)
			.expireAfterWrite(10, TimeUnit.MINUTES).build();


	final static String SIGNATURE_ALGORITHM = "GOST3411WITHGOST3410-2012-256";

	X509CertificateHolder certHolder;
	PrivateKey privateKey;

	public Signer(PrivateKey privateKey, X509CertificateHolder certHolder) {
		this.certHolder = certHolder;
		this.privateKey = privateKey;
	}

	public Signer() {
		this(SystemProperties.get(Signer.class.getName().concat(".keyPath"), "/etc/nginx/ssl/example.key"),
				SystemProperties.get(Signer.class.getName().concat(".certPath"), "/etc/nginx/ssl/example.crt"));
	}

	public Signer(String keyPath, String certPath) {
		try {
			privateKey = privateKeyCache.get(keyPath, () -> {
				PEMParser pemReader;
				Object obj;
				try (FileReader fileReader = new FileReader(keyPath)) {
					pemReader = new PEMParser(fileReader);
					obj = pemReader.readObject();
					pemReader.close();
				}

				if (obj instanceof PEMKeyPair) {
					return new JcaPEMKeyConverter().getKeyPair((PEMKeyPair) obj).getPrivate();
				} else {
					return new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) obj);
				}
			});

			certHolder = certificateHolderCache.get(certPath, () -> {
				PEMParser certPemReader;
				Object certObj;
				try (FileReader fileCertReader = new FileReader(certPath)) {
					certPemReader = new PEMParser(fileCertReader);
					certObj = certPemReader.readObject();
					certPemReader.close();
				}

				return ((X509CertificateHolder) certObj);
			});
		} catch (ExecutionException e) {
			logger.error("error getting certificate or key", e);
		}
	}


	@SuppressWarnings("rawtypes")
	public String signString(String data)  {
		String encoded = null;
		Security.addProvider(new BouncyCastleProvider());

		List<X509CertificateHolder> certList = new ArrayList<>();
		CMSTypedData msg = new CMSProcessableByteArray(data.getBytes()); 

		certList.add(certHolder); // Adding the X509 Certificate
		try {
			Store certs = new JcaCertStore(certList);
	
			CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
			// Initializing the the BC's Signer
			ContentSigner shaSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
					.setProvider("BC").build(privateKey);
	
			gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
					new JcaDigestCalculatorProviderBuilder().setProvider("BC")
							.build()).build(shaSigner, certHolder));
	
			gen.addCertificates(certs);
	
			CMSSignedData sigData = gen.generate(msg, false);
			sigData.getSignerInfos();
			
			encoded = Base64.encodeBase64URLSafeString(sigData.getEncoded());
		} catch(Exception e) {
			logger.error("error sign string{} {}", data, e.toString());
		}

		return encoded;
	}
}