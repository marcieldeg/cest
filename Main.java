import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
	static private SSLSocketFactory socketFactory() {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		try {
			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			SSLSocketFactory result = sslContext.getSocketFactory();

			return result;
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new RuntimeException("Failed to create a SSL socket factory", e);
		}
	}

	public static void main(String[] args) throws Exception {
		String url = "https://www.confaz.fazenda.gov.br/legislacao/convenios/2018/CV142_18";

		Document doc = Jsoup.connect(url).sslSocketFactory(socketFactory()).get();
		Elements tables = doc.getElementsByTag("table");

		List<Cest> cests = new ArrayList<>();
		for (Element table : tables) {
			Elements trs = table.getElementsByTag("tbody").get(0).getElementsByTag("tr");
			for (Element tr : trs) {
				Elements tds = tr.getElementsByTag("td");
				if (tds.size() == 4 && !"ITEM".equals(tds.get(0).text())) {
					// textos em verde são descrições antigas, desconsideradas
					if ("A9-2Tabelajustificadoverde".equals(tds.get(0).getElementsByTag("p").get(0).attr("class")))
						continue;
					// ignora revogados
					if ("REVOGADO".equals(tds.get(1).text()) || "REVOGADO".equals(tds.get(3).text()))
						continue;
					Cest cest = new Cest();
					cest.item = tds.get(0).text();
					cest.cest = tds.get(1).text();
					cest.ncms = tds.get(2).text().split(" ");
					cest.descricao = tds.get(3).text();
					cests.add(cest);
				}
			}
		}
		byte[] json = new ObjectMapper().writeValueAsBytes(cests);
		Files.write(new File("C:\\Users\\Marciel\\Desktop\\cests.json").toPath(), json, StandardOpenOption.CREATE_NEW);
	}

	public static class Cest {
		public String item;
		public String cest;
		public String[] ncms;
		public String descricao;
	}
}
