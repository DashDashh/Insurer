from kafka import KafkaConsumer, KafkaProducer
import json

# Consumer из systems.insurance_system
consumer = KafkaConsumer(
    'systems.insurance_system',
    bootstrap_servers='localhost:9092',
    auto_offset_reset='earliest',
    enable_auto_commit=True
)

# Producer в component.agregator_analytics
producer = KafkaProducer(
    bootstrap_servers='localhost:9092',
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

print("🔄 Relaying messages from systems.insurance_system to component.agregator_analytics")

for message in consumer:
    try:
        # Извлекаем payload из сообщения
        data = json.loads(message.value.decode('utf-8'))
        
        # Если сообщение содержит поле payload, отправляем его содержимое
        if 'payload' in data:
            producer.send('component.agregator_analytics', value=data['payload'])
            print(f"✅ Relayed: {data['action']}")
        else:
            # Иначе отправляем как есть
            producer.send('component.agregator_analytics', value=data)
            print(f"✅ Relayed: {data}")
            
    except Exception as e:
        print(f"❌ Error: {e}")