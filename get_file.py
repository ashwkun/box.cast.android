import json

log_path = '/Users/aswinc/.gemini/antigravity/brain/9a72048a-4681-4243-afc3-fec2c5c9fb77/.system_generated/logs/overview.txt'
with open(log_path, 'r') as f:
    for line in f:
        try:
            data = json.loads(line)
            if 'content' in data:
                if 'cx.aswin.boxcast.feature.home.components.TimeBlockSection' in data['content']:
                    print(f"FOUND IN STEP {data.get('step_index')}")
        except Exception as e:
            pass
