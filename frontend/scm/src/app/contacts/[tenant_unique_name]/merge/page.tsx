import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { Tenant as TenantModel } from "@/models/Tenant";
import DuplicateContacts from "@/Components/Contact/DuplicateContacts";
import React from "react";

const fetchTenant = async (tenant_unique_name: string, IdToken: string): Promise<TenantModel> => {
    try {
        const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/tenants/unique/${tenant_unique_name}`, {
            headers: {
                "userToken": `Bearer ${IdToken}`,
            },
        });

        if (!res.ok) {
            throw new Error(`Error fetching tenant: ${res.statusText}`);
        }

        return await res.json();
    } catch (error) {
        console.error("Failed to fetch tenant:", error);
        return {} as TenantModel;
    }
};

const DuplicateContactsPage = async (props: { params: { tenant_unique_name: string } }) => {
    const { params } = props;
    const { tenant_unique_name } = params;
    const IdToken = cookies().get("IdToken")?.value || "";

    if (!IdToken) {
        redirect("/login");
    }

    const tenant = await fetchTenant(tenant_unique_name, IdToken);

    return (
        <>
            <head>
                <title>SCM - Duplicate Contacts</title>
            </head>
            <div className="container mx-auto p-4">
                <DuplicateContacts IdToken={IdToken} tenantUniqueName={tenant_unique_name} />
            </div>
        </>
    );
};

export default DuplicateContactsPage;