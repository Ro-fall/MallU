import json
import urllib.request

OPENAPI_URL = "http://localhost:8080/v3/api-docs"
OUTPUT_FILE = "API.md"


def get_type_str(schema):
    if not schema:
        return ""
    if "type" in schema:
        t = schema["type"]
        if t == "array" and "items" in schema:
            item_type = schema["items"].get("type", "object")
            return f"array[{item_type}]"
        return t
    if "$ref" in schema:
        ref = schema["$ref"]
        return ref.split("/")[-1]
    return "object"


def generate_md(data):
    lines = []
    info = data.get("info", {})
    lines.append(f"# {info.get('title', 'API 文档')}")
    lines.append("")
    lines.append(f"**版本**: {info.get('version', '-')}")
    lines.append("")
    lines.append(f"{info.get('description', '')}")
    lines.append("")
    lines.append("## 认证方式")
    lines.append("")
    lines.append("大部分接口需要 JWT Token，请在请求头中携带：")
    lines.append("")
    lines.append("```http")
    lines.append("Authorization: Bearer {token}")
    lines.append("```")
    lines.append("")
    lines.append("---")
    lines.append("")

    paths = data.get("paths", {})
    components = data.get("components", {}).get("schemas", {})

    # Group by tag
    grouped = {}
    for path, methods in paths.items():
        for method, detail in methods.items():
            tag = (detail.get("tags") or ["未分类"])[0]
            grouped.setdefault(tag, []).append((path, method, detail))

    for tag, endpoints in grouped.items():
        lines.append(f"## {tag}")
        lines.append("")
        for path, method, detail in endpoints:
            summary = detail.get("summary", "")
            op_id = detail.get("operationId", "")
            desc = detail.get("description", "")
            lines.append(f"### {summary or op_id}")
            lines.append("")
            lines.append(f"- **请求方式**: `{method.upper()}`")
            lines.append(f"- **请求路径**: `{path}`")
            if desc:
                lines.append(f"- **说明**: {desc}")
            lines.append("")

            # Parameters
            params = detail.get("parameters", [])
            if params:
                lines.append("**请求参数**:")
                lines.append("")
                lines.append("| 参数名 | 位置 | 类型 | 必填 | 说明 |")
                lines.append("|--------|------|------|------|------|")
                for p in params:
                    name = p.get("name", "")
                    loc = p.get("in", "")
                    required = "是" if p.get("required") else "否"
                    schema = p.get("schema", {})
                    t = get_type_str(schema)
                    desc = p.get("description", "")
                    lines.append(f"| {name} | {loc} | {t} | {required} | {desc} |")
                lines.append("")

            # Request body
            request_body = detail.get("requestBody", {})
            if request_body:
                content = request_body.get("content", {})
                for ct, body in content.items():
                    schema = body.get("schema", {})
                    lines.append(f"**请求体类型**: `{ct}`")
                    lines.append("")
                    lines.append(f"```json")
                    lines.append(json.dumps(build_example(schema, components), ensure_ascii=False, indent=2))
                    lines.append(f"```")
                    lines.append("")
                    break

            # Responses
            responses = detail.get("responses", {})
            if responses:
                lines.append("**响应**:")
                lines.append("")
                for code, resp in responses.items():
                    desc = resp.get("description", "")
                    lines.append(f"- `{code}`: {desc}")
                lines.append("")

            lines.append("---")
            lines.append("")

    # Schemas
    if components:
        lines.append("## 数据模型")
        lines.append("")
        for name, schema in components.items():
            lines.append(f"### {name}")
            lines.append("")
            props = schema.get("properties", {})
            if props:
                lines.append("| 字段 | 类型 | 说明 |")
                lines.append("|------|------|------|")
                for prop_name, prop_schema in props.items():
                    t = get_type_str(prop_schema)
                    desc = prop_schema.get("description", "")
                    lines.append(f"| {prop_name} | {t} | {desc} |")
                lines.append("")

    return "\n".join(lines)


def build_example(schema, components, depth=0):
    if depth > 3:
        return {}
    if not schema:
        return {}
    if "$ref" in schema:
        ref = schema["$ref"].split("/")[-1]
        comp = components.get(ref, {})
        return build_example(comp, components, depth + 1)
    t = schema.get("type", "object")
    if t == "object":
        result = {}
        for prop, prop_schema in schema.get("properties", {}).items():
            result[prop] = build_example(prop_schema, components, depth + 1)
        return result
    elif t == "array":
        return [build_example(schema.get("items", {}), components, depth + 1)]
    elif t == "string":
        return "string"
    elif t == "integer":
        return 0
    elif t == "number":
        return 0.0
    elif t == "boolean":
        return False
    return None


def main():
    try:
        with urllib.request.urlopen(OPENAPI_URL, timeout=10) as resp:
            data = json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"获取 OpenAPI 数据失败: {e}")
        print(f"请确认项目已启动，且能访问 {OPENAPI_URL}")
        return

    md = generate_md(data)
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write(md)
    print(f"已生成接口文档: {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
