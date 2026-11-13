package dev.pagewatch.monitor.component;


import com.github.difflib.DiffUtils;
import org.springframework.stereotype.Component;
import java.util.*;
import static dev.pagewatch.monitor.model.Models.*;

@Component
public class DiffService {
    public List<DiffLine> compare(String before, String after) {
        var oldLines = before.isEmpty() ? List.<String>of() : before.lines().toList();
        var newLines = after.isEmpty() ? List.<String>of() : after.lines().toList();
        var deltas = DiffUtils.diff(oldLines, newLines).getDeltas().stream()
                .sorted(Comparator.comparingInt(d -> d.getSource().getPosition())).toList();
        List<DiffLine> result = new ArrayList<>();
        int oldIndex = 0, newIndex = 0;
        for (var delta : deltas) {
            while (oldIndex < delta.getSource().getPosition())
                result.add(new DiffLine("equal", oldLines.get(oldIndex), ++oldIndex, ++newIndex));
            for (String line : delta.getSource().getLines()) result.add(new DiffLine("remove", line, ++oldIndex, null));
            for (String line : delta.getTarget().getLines()) result.add(new DiffLine("add", line, null, ++newIndex));
        }
        while (oldIndex < oldLines.size()) result.add(new DiffLine("equal", oldLines.get(oldIndex), ++oldIndex, ++newIndex));
        return result;
    }
}
