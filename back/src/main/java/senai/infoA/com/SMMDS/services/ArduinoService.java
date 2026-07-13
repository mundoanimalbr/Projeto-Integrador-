package senai.infoA.com.SMMDS.services;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Scanner;

import org.springframework.stereotype.Service;

import com.fazecast.jSerialComm.SerialPort;

import senai.infoA.com.SMMDS.models.Leitura;
import senai.infoA.com.SMMDS.models.Sensor;
import senai.infoA.com.SMMDS.models.Recomendacao;
import senai.infoA.com.SMMDS.repositories.LeituraRepository;
import senai.infoA.com.SMMDS.repositories.RecomendacaoRepository;
import senai.infoA.com.SMMDS.repositories.SensorRepository;


@Service
public class ArduinoService {
    private final SensorRepository sensorRepository;
    private final LeituraRepository leituraRepository;
    private final RecomendacaoRepository recomendacaoRepository;
    private final GroqService groqService;

    public ArduinoService(SensorRepository sensorRepository, LeituraRepository leituraRepository, RecomendacaoRepository recomendacaoRepository, GroqService groqService) {
        this.sensorRepository = sensorRepository;
        this.leituraRepository = leituraRepository;
        this.recomendacaoRepository = recomendacaoRepository;
        this.groqService = groqService;
    }

    public void iniciarLeitura(){
        SerialPort porta= SerialPort.getCommPort("/dev/ttyUSB0");
        porta.setBaudRate(9600);
    if (porta.openPort()){
        System.out.println("Arduíno conectado!");  //Define velocidade de comunicação (9600) e abre as portas para o arduíno
        try (Scanner scanner = new Scanner(porta.getInputStream())){  //Java assume o controle de comunicação entre o arduíno
            while (scanner.hasNextLine()) {
                String linha = scanner.nextLine();
                processarLinha (linha); //Enquanto houver comunicação, os dados serão enviados para a programação "processar linha" (definida em baixo)
            }
        }catch (Exception e){
            System.out.println("Erro na leitura dos dados:" + e.getMessage()); // Se o cabo for retirado ou falha na comunicação, mostra o erro e a mensagem
        } finally {
            porta.closePort();    // Independente do que aconteceu antes, faça o que está antes de mim
        }
    }
    else {
        System.out.println("Erro ao conectar USB");
        }
    }
    private void processarLinha(String linha){
        
        if (linha == null || linha.trim().isEmpty()) {
        return;
    }

    String[] partes = linha.split(";");
    for (String parte : partes) {
        
        // CORREÇÃO: Se o pedaço da linha não contiver ":", ignora (vai salvar sua 4ª parte vazia)
        if (!parte.contains(":")) {
            continue; 
        }

        String[] chaveDado = parte.split(":");
        
        // CORREÇÃO: Garante que temos a chave E o dado antes de continuar
        if (chaveDado.length < 2) {
            continue;
        }

        String tipoSensor = chaveDado[0].replace("\0", "").trim();
        // 1. Pegamos o texto bruto do valor (ex: "45.2")
        String valorBruto = chaveDado[1];

        // 2. Remove TUDO o que não for número ou ponto (limpa o \r, \n, espaços, etc.)
        String valorLimpo = valorBruto.replaceAll("[^0-9.]", "");

        // 3. Validação: se ficou vazio ou se veio com mais de um ponto por erro de ruído, ignora
        if (valorLimpo.isEmpty() || valorLimpo.indexOf('.') != valorLimpo.lastIndexOf('.')) {
            System.out.println("Ignorando valor numérico inválido: [" + valorBruto.trim() + "]");
            continue; 
        }

        // 4. Agora sim, transformamos em BigDecimal com total segurança!
        BigDecimal dado = new BigDecimal(valorLimpo);
    Sensor sensor = sensorRepository.findByTipoSensor(tipoSensor).orElse(null);
    if (sensor == null){
        System.out.println("Sensor não encontrado:" + tipoSensor);
        continue;
    }
        

    Leitura leitura = new Leitura();
    leitura.setSensor(sensor);
    leitura.setDado(dado);
    leitura.setUltimaAtualizacao(new Timestamp(System.currentTimeMillis()));
    leitura.setClassificacaoDado(classificar(dado));
    leitura.setRiscoDado(calcularRisco(tipoSensor, dado));
   
    String tipoSolo; if (sensor.getLocal() != null) { tipoSolo = sensor.getLocal().getTipoSolo();} else {tipoSolo= "Não especificado";} //Tem um local associado a esse sensor? Se sim, porcure o tipo de solo. Se não, o tipo não foi especificado.
    String textoGerado = groqService.pedirRecomendacaoAgricola(tipoSensor, dado, tipoSolo);
   Recomendacao recomendacao = new Recomendacao();
        recomendacao.setTextoRecomendacao(textoGerado);
    Recomendacao recomendacaoSalva = recomendacaoRepository.save(recomendacao);
            leitura.setRecomendacao(recomendacaoSalva);
            leituraRepository.save(leitura);
            System.out.println("Leitura salva no banco com a recomendação da IA!");
    }
    }
        
    private String classificar (BigDecimal dado){
        if (dado.compareTo(new BigDecimal("40")) < 0){
        return "Baixo";
    }
    // Substitui: else if (valor < 70)
    else if (dado.compareTo(new BigDecimal("70")) < 0){
        return "Moderado";
    }
    return "Alto";
}

    private String calcularRisco(String tipoSensor,BigDecimal dado){
        if(tipoSensor.equals("UMID_SOLO")){
        // Substitui: if (valor > 70)
        if(dado.compareTo(new BigDecimal("70")) > 0){
            return "Alto";
        }
        // Substitui: else if (valor > 40)
        else if(dado.compareTo(new BigDecimal("40")) > 0){
            return "Moderado";
        }
        return "Baixo";
    }
    return "Sem risco";
}
}



    

