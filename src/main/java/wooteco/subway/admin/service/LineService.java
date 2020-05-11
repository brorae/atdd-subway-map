package wooteco.subway.admin.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import wooteco.subway.admin.domain.Line;
import wooteco.subway.admin.domain.LineStation;
import wooteco.subway.admin.dto.LineResponse;
import wooteco.subway.admin.dto.LineStationCreateRequest;
import wooteco.subway.admin.dto.StationResponse;
import wooteco.subway.admin.exception.ExistingNameException;
import wooteco.subway.admin.repository.LineRepository;
import wooteco.subway.admin.repository.StationRepository;

@Service
public class LineService {
    private LineRepository lineRepository;
    private StationRepository stationRepository;

    public LineService(LineRepository lineRepository, StationRepository stationRepository) {
        this.lineRepository = lineRepository;
        this.stationRepository = stationRepository;
    }

    public Long save(Line line) {
        validateLine(line);
        return lineRepository.save(line).getId();
    }

    private void validateLine(Line lineToCreate) {
        boolean exist = lineRepository.existsLineBy(lineToCreate.getName().trim());
        if (exist) {
            throw new ExistingNameException(lineToCreate.getName());
        }
    }

    public List<LineResponse> showLines() {
        return lineRepository.findAll().stream()
            .map(this::from)
            .collect(Collectors.toList());
    }

    public void updateLine(Long id, Line line) {
        Line persistLine = lineRepository.findById(id).orElseThrow(RuntimeException::new);
        persistLine.update(line);
        lineRepository.save(persistLine);
    }

    public void deleteLineById(Long id) {
        lineRepository.deleteById(id);
    }

    public void addLineStation(Long id, LineStationCreateRequest request) {
        Line line = findById(id);
        LineStation lineStation = request.toLineStation();
        line.addLineStation(lineStation);
        lineRepository.save(line);
    }

    public void removeLineStation(Long lineId, Long stationId) {
        Line line = findById(lineId);
        line.removeLineStationById(stationId);
        lineRepository.save(line);
    }

    public List<StationResponse> findStationsByLineId(Long id) {
        Line line = findById(id);
        return getStationResponses(line.getLineStationsId());
    }

    public LineResponse findLineWithStationsById(Long id) {
        Line line = findById(id);
        return from(line);
    }

    private LineResponse from(Line line) {
        List<Long> stationsId = line.getLineStationsId();
        List<StationResponse> stations = getStationResponses(stationsId);
        return LineResponse.of(line, stations);
    }

    private List<StationResponse> getStationResponses(List<Long> stationsId) {
        return stationsId.stream()
            .map(stationRepository::findById)
            .map(Optional::get)
            .map(StationResponse::of)
            .collect(Collectors.toList());
        // List<Station> stations = stationRepository.findAllById(stationsId);
        // return stations.stream()
        //     .map(StationResponse::of)
        //     .collect(Collectors.toList());
    }

    private Line findById(Long id) {
        return lineRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당하는 id가 없습니다."));
    }
}
