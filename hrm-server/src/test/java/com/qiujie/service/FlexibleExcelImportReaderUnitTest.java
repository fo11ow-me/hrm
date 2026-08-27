package com.qiujie.service;

import com.qiujie.dto.AttendanceImportRow;
import com.qiujie.enums.TaskModuleEnum;
import com.qiujie.util.TestExcelUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlexibleExcelImportReaderUnitTest {

    @TempDir
    Path tempDir;

    @Test
    void read_ShouldConvertRowsWithoutColumnOrderDependency() {
        File source = TestExcelUtil.createAttendanceImportExcel(
                tempDir.resolve("attendance.xlsx").toString(), 2, "20240101");

        List<ImportReader.ImportBatch<AttendanceImportRow>> batches = new ArrayList<>();
        new FlexibleExcelImportReader<>(1, TaskModuleEnum.ATTENDANCE,
                AttendanceImportRow.class, null)
                .read(source, 7L, batches::add);

        assertThat(batches).hasSize(1);
        assertThat(batches.get(0).rows()).hasSize(2);
        assertThat(batches.get(0).errors()).isEmpty();
    }
}
