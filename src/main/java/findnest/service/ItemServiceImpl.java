package findnest.service;

import com.google.api.core.ApiFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import findnest.model.Items;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.Map;

@Service
public class ItemServiceImpl implements ItemService {

    private final DatabaseReference dbRef;
    private final DatabaseReference historyDbRef;

    public ItemServiceImpl() {
        this.dbRef = FirebaseDatabase.getInstance().getReference("items");
        this.historyDbRef = FirebaseDatabase.getInstance().getReference("ItemsHistory");
    }

    @Override
    public Items saveItem(Items item) {
        String id = dbRef.push().getKey();
        item.setId(id);
        String timestamp = Instant.now().toString();
        item.setCreatedAt(timestamp);
        item.setUpdatedAt(timestamp);
        item.setStatus("Available");

        // Ensure turnover fields are set
        if (item.getTurnoverDate() == null) {
            item.setTurnoverDate(""); // Default empty value
        }
        if (item.getTurnoverPerson() == null) {
            item.setTurnoverPerson(""); // Default empty value
        }

        dbRef.child(id).setValueAsync(item);

        return item;
    }

    @Override
    public Items getItemById(String id) {
        CompletableFuture<Items> future = new CompletableFuture<>();
        dbRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    future.complete(snapshot.getValue(Items.class));
                } else {
                    future.complete(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });

        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Items> getAllItems() {
        CompletableFuture<List<Items>> future = new CompletableFuture<>();
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Items> itemsList = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        Items item = childSnapshot.getValue(Items.class);
                        itemsList.add(item);
                    }
                }
                future.complete(itemsList);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });

        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public Items updateItem(String id, Items item) {
        CompletableFuture<Items> future = new CompletableFuture<>();

        dbRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    item.setId(id);
                    item.setUpdatedAt(Instant.now().toString());

                    // Ensure turnover fields are set
                    if (item.getTurnoverDate() == null) {
                        item.setTurnoverDate(""); // Default empty value
                    }
                    if (item.getTurnoverPerson() == null) {
                        item.setTurnoverPerson(""); // Default empty value
                    }

                    dbRef.child(id).setValueAsync(item);
                    future.complete(item);
                } else {
                    future.complete(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });

        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void deleteItem(String id) {
        dbRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Items item = snapshot.getValue(Items.class);
                    if (item != null) {
                        saveToItemsHistory(item);
                    }
                    dbRef.child(id).removeValueAsync();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                error.toException().printStackTrace();
            }
        });
    }

    @Override
    public void saveToItemsHistory(Items item) {
        String historyId = historyDbRef.push().getKey();
        item.setId(historyId);
        historyDbRef.child(historyId).setValueAsync(item);
    }

    @Override
    public List<Items> getAllItemsFromHistory() {
        CompletableFuture<List<Items>> future = new CompletableFuture<>();
        historyDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Items> itemsList = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        Items item = childSnapshot.getValue(Items.class);
                        itemsList.add(item);
                    }
                }
                future.complete(itemsList);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });

        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public Items getItemFromHistoryById(String id) {
        CompletableFuture<Items> future = new CompletableFuture<>();
        historyDbRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    future.complete(snapshot.getValue(Items.class));
                } else {
                    future.complete(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });

        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Items countAllItems() {
        CompletableFuture<Items> future = new CompletableFuture<>();
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Items itemCounts = new Items();
                itemCounts.setTotalCount(snapshot.getChildrenCount());
                long availableCount = 0;
                long claimedCount = 0;

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String status = childSnapshot.child("status").getValue(String.class);
                    if ("Available".equalsIgnoreCase(status)) {
                        availableCount++;
                    } else if ("Claimed".equalsIgnoreCase(status)) {
                        claimedCount++;
                    }
                }

                itemCounts.setAvailableCount(availableCount);
                itemCounts.setClaimedCount(claimedCount);
                future.complete(itemCounts);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });

        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return new Items();
        }
    }

    @Override
    public Items patchItem(String id, Map<String, Object> updates) {
        CompletableFuture<Items> future = new CompletableFuture<>();
        dbRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Items item = snapshot.getValue(Items.class);
                    if (item != null) {
                        // Update turnover fields if present in updates
                        if (updates.containsKey("turnoverDate")) {
                            item.setTurnoverDate((String) updates.get("turnoverDate"));
                        }
                        if (updates.containsKey("turnoverPerson")) {
                            item.setTurnoverPerson((String) updates.get("turnoverPerson"));
                        }
    
                        // Update claimant fields
                        if (updates.containsKey("claimantName")) {
                            item.setClaimantName((String) updates.get("claimantName"));
                        }
                        if (updates.containsKey("claimantImage")) {
                            item.setClaimantImage((String) updates.get("claimantImage"));
                        }
    
                        // Ensure status is updated to "Claimed" if claimant info is provided
                        if (item.getClaimantName() != null && !item.getClaimantName().isEmpty()) {
                            item.setStatus("Claimed");
                        }
    
                        // Set the updated timestamp
                        item.setUpdatedAt(Instant.now().toString());
    
                        // Perform the asynchronous update in Firebase using ApiFuture
                        ApiFuture<Void> futureUpdate = dbRef.child(id).setValueAsync(item);
    
                        // Add a listener to handle completion or failure
                        futureUpdate.addListener(() -> {
                            try {
                                futureUpdate.get(); // Ensure the operation completes successfully
                                future.complete(item);
                            } catch (Exception e) {
                                future.completeExceptionally(e); // Handle the exception
                            }
                        }, Executors.newSingleThreadExecutor());
                    } else {
                        future.complete(null); // Item not found
                    }
                } else {
                    future.complete(null); // Snapshot does not exist
                }
            }
    
            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });
    
        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}    