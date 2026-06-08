package senai.infoA.com.SMMDS.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import senai.infoA.com.SMMDS.models.Leitura;
import senai.infoA.com.SMMDS.models.Sensor;
import senai.infoA.com.SMMDS.repositories.LeituraRepository;

@Service
public class LeituraService {


    @Autowired
    private LeituraRepository leituraRepository;

    @Autowired
    private SensorService sensorService;

      public List<Leitura> listarTodas() {
        return leituraRepository.findAll();
    }

    // Buscar por ID
    public Leitura buscarPorId(Integer id) {
        return leituraRepository.findById(id).get();
    }

    // Salvar leitura
    public Leitura salvar(Leitura leitura) {
        Sensor sensorLeitura = sensorService.buscarPorId(leitura.getSensor().getSensorId());
        leitura.setSensor(sensorLeitura);
        return leituraRepository.save(leitura);
    }

    
    public Leitura atualizar(Integer id, Leitura leituraAtualizada) {

       Leitura leitura = buscarPorId(id);


        leitura.setDado(leituraAtualizada.getDado());
        leitura.setUltimaAtualizacao(leituraAtualizada.getUltimaAtualizacao());
        leitura.setRiscoDado(leituraAtualizada.getRiscoDado());
        leitura.setClassificacaoDado(leituraAtualizada.getClassificacaoDado());

        return leituraRepository.save(leitura);
    } 

    // Deletar leitura
    public void deletar(Integer id) {
        leituraRepository.deleteById(id);
    }
    
}
