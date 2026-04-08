def validate_required_fields(payload: dict, fields: list):
    missing = [f for f in fields if f not in payload]

    if missing:
        raise ValueError(f"Missing required fields: {missing}")