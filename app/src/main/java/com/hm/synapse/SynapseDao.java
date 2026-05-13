package com.hm.synapse;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SynapseDao {
    // Modified to put AI suggestions at the very top, and newest items first within groups
    @Query("SELECT * FROM blocks WHERE userId = :userId ORDER BY (type = 'AI_SUGGESTION') DESC, timestamp DESC")
    LiveData<List<SynapseBlockEntity>> getAllBlocks(String userId);

    @Query("SELECT * FROM blocks WHERE userId = :userId ORDER BY (type = 'AI_SUGGESTION') DESC, timestamp DESC")
    List<SynapseBlockEntity> getAllBlocksSync(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SynapseBlockEntity block);

    @Update
    void update(SynapseBlockEntity block);

    @Delete
    void delete(SynapseBlockEntity block);

    @Query("DELETE FROM blocks WHERE id = :blockId")
    void deleteById(String blockId);

    @Query("DELETE FROM blocks WHERE userId = :userId")
    void deleteAllUserBlocks(String userId);
}
