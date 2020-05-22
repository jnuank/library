package library.application.service.retention;

import library.application.repository.RetentionRepository;
import library.domain.model.item.ItemNumber;
import library.domain.model.reservation.retention.Retention;
import org.springframework.stereotype.Service;

/**
 * 取置登録サービス
 */
@Service
public class RetentionRecordService {

    RetentionRepository retentionRepository;

    public RetentionRecordService(RetentionRepository retentionRepository) {
        this.retentionRepository = retentionRepository;
    }

    /**
     * 予約された本を取り置く
     */
    public void registerRetention(Retention retention) {
        retentionRepository.registerRetention(retention);
    }

    /**
     * 取り置いた蔵書を貸し出す(取置を消しこむ)
     */
    public void loaned(ItemNumber itemNumber) {
        retentionRepository.loaned(itemNumber);
    }
}
