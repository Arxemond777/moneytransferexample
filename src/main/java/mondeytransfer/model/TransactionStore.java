package mondeytransfer.model;

import mondeytransfer.dto.UserDto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a layer for transactions data
 *
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public class TransactionStore {

    private final Map<Long, UserDto> STORE = new ConcurrentHashMap<>();

    public Map<Long, UserDto> getSTORE() {
        return STORE;
    }
}
