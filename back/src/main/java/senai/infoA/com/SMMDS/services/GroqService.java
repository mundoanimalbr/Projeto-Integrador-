package senai.infoA.com.SMMDS.services;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GroqService {

    // O Spring Boot vai ler automaticamente a chave que você colocou no application.properties
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final String URL_GROQ = "https://api.groq.com/openai/v1/chat/completions"; // Link para acessar o servidor do Groq
    private final ObjectMapper om = new ObjectMapper();  // Chama o Json

    public String pedirRecomendacaoAgricola(String tipoSensor, BigDecimal dado, String tipoSolo) {
        try {
            // 1. Criamos o "cliente" que vai navegar na internet
            HttpClient cliente = HttpClient.newHttpClient();

           
            String instrucaoIA = "Você é um engenheiro agrônomo e consultor de sustentabilidade especialista. "
                               + "Forneça uma recomendação curta, prática e direta (máximo de 50 palavras) "
                               + "de manejo ou irrigação baseado no sensor."
                               + "Considere o tipo de solo para dar uma resposta mais precisa."
                               + "Traga dicas de sustentabilidade de acordo com os dados recebidos";

            // Montamos a pergunta com os dados reais do seu Arduino
            String perguntaUsuario = "O sensor " + tipoSensor + " marcou o valor " + dado + " no tipo de solo " + tipoSolo + ". O que o agricultor deve fazer?";

            // Montando o formato da mensagem
            Map<String, Object> payload = Map.of(        // Formato que o Groq usa. É um mapa.
                "model", "llama3-8b-8192", "messages", List.of(
                    Map.of("role","system","content", instrucaoIA),
                    Map.of("role","user","content", perguntaUsuario)
                )
            );
            String corpoJson = om.writeValueAsString(payload); // Transforma o mapa em texto Json

            //  Preparamos a viagem dos dados colocando a sua chave de segurança (chave de API) no header
            HttpRequest requisicao = HttpRequest.newBuilder()
                    .uri(URI.create(URL_GROQ))
                    .timeout(Duration.ofSeconds(20)) // Tempo para a IA responder 
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(corpoJson, StandardCharsets.UTF_8)) //Envia os dados e traz o Json com caracteres como acentos
                    .build();

            // 6Fazemos a ligação web e aguardamos o Groq responder
            HttpResponse<String> resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());

        
            int status = resposta.statusCode();
            if (status >= 200 && status < 300) {
                return extrairTextoDaResposta(resposta.body()); //Status na faixa dos 200: chama o método de resposta
            } else if (status == 401 || status == 403) {  // Status que indicam que a API não foi autenticada
                return "A IA não foi autenticada.";
            } else {                                      //Qualquer outro status
                return "IA respondeu com o status" + status + ".";
            }

        } catch (Exception e) {
            return "Erro ao conectar com a IA: " + e.getMessage();  // Por exemplo, se a internet cair, mostra o erro e motivo do problema
        }
    }

    // Método auxiliar para recortar o JSON e pegar apenas a frase de resposta da IA
    private String extrairTextoDaResposta(String jsonResposta) {
        try {
           JsonNode root = om.readTree(jsonResposta);   //Pega a árvore de nós da resposta e coloca em Json
           JsonNode choices = root.path("choices");  // "Choices" é onde fica guardado as opções de respostas das IAs (OpenIA)
           if (choices.isArray() && choices.size()>0){  // Verifica se a resposta tem um valor
               JsonNode content = choices.get(0).path("message").path("content"); // Se sim, percorre um caminho até chegar no content (onde a resposta fica)
               if (!content.isMissingNode()) return content.asText().replace("\n", " ").trim(); // Transforma em texto e o deixa corrido
            }
            JsonNode anyContent = root.findValue("content");
            if (anyContent != null) return anyContent.asText().replace("\n", " ").trim(); //Se der algum erro, ele começa a buscar qualquer campo content
        } catch (Exception e) {
        }
        return "Leitura registrada."; //Não conseguiu capturar, só diz que teve uma leitura registrada mas não mostra a reposta
    }
}